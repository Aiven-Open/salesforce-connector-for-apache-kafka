/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aiven.kafka.connect.salesforce.common.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aiven.commons.util.strings.HttpStatus;
import io.aiven.kafka.connect.salesforce.common.auth.credentials.Oauth2Login;
import io.aiven.kafka.connect.salesforce.common.bulk.query.AbortJob;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;
import io.aiven.kafka.connect.salesforce.common.utils.TestingSalesforceCommonConfig;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Test the Bulk Api Client to confirm it behaves in an expected way. */
public class BulkApiClientTest {

  public static final String TEST_CLIENT_ID = "test_client_id";
  public static final String TEST_CLIENT_SECRET = "test_client_secret";
  public static final String TEST_OAUTH_URI = "https://localhost:1234/oauth";
  public static final String TEST_SALESFORCE_URI = "https://localhost:1234";
  public static final String TEST_ACCESS_TOKEN = "test_access_token";
  public static final String TEST_GRANT_TYPE = "client_credentials";
  public static final String TEST_JOB_ID = "test-job";
  public static final String SALESFORCE_API_VERSION = "v65.0";
  public static final String BEARER_TOKEN = "abcd12345";

  private Oauth2Login login;

  private HttpClient client;

  private io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient apiClient;

  private Map<String, String> props;

  ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  public void setup() {
    login = mock(Oauth2Login.class);
    client = mock(HttpClient.class);
    props = new HashMap<>();
    // set the testing property defaults.
    SalesforceCommonConfigFragment.setter(props)
        .oauthClientId(TEST_CLIENT_ID)
        .oauthClientSecret(TEST_CLIENT_SECRET)
        .oauthUri(TEST_OAUTH_URI)
        .uri(TEST_SALESFORCE_URI);
  }

  private BulkApiClient createClient() {
    return new BulkApiClient(new TestingSalesforceCommonConfig(props), client, login);
  }

  /**
   * Materializes an {@link BodyPublisher} from {@link HttpRequest#bodyPublisher()} for assertions.
   *
   * <p>Note that this technique only works while the HTTP request is being mocked. If it were
   * executed by the HttpClient, the body would have been consumed.
   */
  private static String extractMockBody(BodyPublisher publisher) {
    CompletableFuture<byte[]> done = new CompletableFuture<>();
    publisher.subscribe(
        new Flow.Subscriber<>() {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();

          @Override
          public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
          }

          @Override
          public void onNext(ByteBuffer buffer) {
            byte[] arr = new byte[buffer.remaining()];
            buffer.get(arr);
            out.writeBytes(arr);
          }

          @Override
          public void onError(Throwable throwable) {
            done.completeExceptionally(throwable);
          }

          @Override
          public void onComplete() {
            done.complete(out.toByteArray());
          }
        });
    return new String(done.join(), StandardCharsets.UTF_8);
  }

  /** Tests the successful path for doing a multipart insert. */
  @Test
  public void testMultipartInsert() {
    apiClient = createClient();
    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);

    String responseBody =
        """
            { "id":"000INSERTID000",
              "object":"Account",
              "operation": "insert" }
            """;

    HttpResponse<Object> mockQueryResponse = mockResponse(responseBody, HttpStatus.SC_OK);

    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockQueryResponse));

    Optional<QueryResponse> result =
        apiClient.multipartInsert(
            "Account",
            new String[] {"AccountNumber", "Name"},
            Stream.of(new Object[] {"1", "Test1"}, new Object[] {"2", "Test2, Inc"}));

    // Testing the parsed response
    assertThat(result)
        .hasValueSatisfying(
            response -> {
              assertThat(response).hasFieldOrPropertyWithValue("id", "000INSERTID000");
              assertThat(response).hasFieldOrPropertyWithValue("object", "Account");
            });

    // Testing that the request was formed as expected
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(client).sendAsync(captor.capture(), any());
    HttpRequest sent = captor.getValue();
    assertThat(sent.method()).isEqualTo("POST");
    URI expectedUri =
        URI.create(
            TEST_SALESFORCE_URI
                + String.format(BulkApiClient.ingestJobUri, SALESFORCE_API_VERSION));
    assertThat(sent.uri()).isEqualTo(expectedUri);
    assertThat(sent.headers().firstValue("Authorization"))
        .hasValue(BulkApiClient.BEARER + BEARER_TOKEN);

    String[] reqContentType =
        sent.headers().firstValue("Content-Type").orElseThrow().split("\"", -1);
    assertThat(reqContentType).hasSize(3);
    assertThat(reqContentType[0]).isEqualTo("multipart/form-data; boundary=");
    assertThat(reqContentType[1]).matches("[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}");
    assertThat(reqContentType[2]).isBlank();

    String body = extractMockBody(captor.getValue().bodyPublisher().orElseThrow());
    assertThat(body)
        .isEqualTo(
            String.format(
                """
            --%1$s
            Content-Type: application/json
            Content-Disposition: form-data; name="job"

            {"object":"Account","contentType":"CSV","operation":"insert","lineEnding":"LF"}

            --%1$s
            Content-Type: text/csv
            Content-Disposition: form-data; name="content"; filename="content"

            AccountNumber,Name
            1,Test1
            2,"Test2, Inc"
            --%1$s--
            """,
                reqContentType[1]));
  }

  @Test
  public void testAutomaticRetryOfCredentials() throws JsonProcessingException {
    apiClient = createClient();
    HttpResponse<Object> resp401 =
        new FakeHttpResponse(HttpStatus.SC_UNAUTHORIZED) {
          // unauthorized processing requires the request.
          @Override
          public HttpRequest request() {
            return mock(HttpRequest.class);
          }
        };
    CompletableFuture<HttpResponse<Object>> future401 = CompletableFuture.completedFuture(resp401);
    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    String body = mapper.writeValueAsString(response);
    HttpResponse<Object> resp200 =
        new FakeHttpResponse(HttpStatus.SC_OK) {
          // OK processing requires the body.
          @Override
          public String body() {
            return body;
          }
        };
    CompletableFuture<HttpResponse<Object>> future200 = CompletableFuture.completedFuture(resp200);
    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET)))
        .thenReturn(TEST_ACCESS_TOKEN);

    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(future401)
        .thenReturn(future200);

    assertThat(apiClient.submitQueryJob("SELECT * FROM ACCOUNT")).hasValue(TEST_JOB_ID);

    // Initial accessToken plus retry
    verify(login, times(2)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Retries and returns successfully the second time
    verify(client, times(2)).sendAsync(any(HttpRequest.class), any());
  }

  @Test
  public void testRetryExpectedNumberOfTimes() throws JsonProcessingException, URISyntaxException {
    SalesforceCommonConfigFragment.setter(props).maxRetries(3);
    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);
    apiClient = createClient();
    HttpResponse<Object> resp500 =
        new FakeHttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR) {
          // error processing requires the request.
          @Override
          public HttpRequest request() {
            return mock(HttpRequest.class);
          }
        };
    CompletableFuture<HttpResponse<Object>> future500 = CompletableFuture.completedFuture(resp500);

    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(future500)
        .thenReturn(future500)
        .thenReturn(future500)
        .thenReturn(future500);

    BulkApiClient.ExecutionResult result =
        apiClient.executeHttpRequest(HttpRequest.newBuilder(new URI("http://example.com"))).join();
    assertThat(result.hasException()).isTrue();
    assertThat(result.getMessage()).isEqualTo("Could not complete request after 3 retries");

    // No 401's so this does not get called other than in the setup.
    verify(login, times(1)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Retries and returns successfully the second time
    // expected verification is 1 initial try + 3 retries + 1 setup = 5
    verify(client, times(5)).sendAsync(any(HttpRequest.class), any());
  }

  @Test
  public void testDeleteJob() throws JsonProcessingException {
    apiClient = createClient();

    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    HttpResponse<Object> mockQueryResponse = mockResponse(mapper.writeValueAsString(response), 200);

    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockQueryResponse));
    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);
    String jobId = apiClient.submitQueryJob("SELECT Id, Name FROM ACCOUNT").get();

    apiClient.deleteJob(jobId).join(); // verify it finished
    // Needs to be updated to initialise the access token correctly.
    var deleteRequest =
        HttpRequest.newBuilder(
                URI.create(
                    TEST_SALESFORCE_URI
                        + String.format(
                            io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient
                                .queryJobByIdUri,
                            SALESFORCE_API_VERSION,
                            jobId)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + BEARER_TOKEN)
            .DELETE()
            .build();

    // No 401's so this does not get called.
    verify(login, times(1)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Submit job and delete job.
    verify(client, times(2)).sendAsync(any(HttpRequest.class), any());

    // specifically ensure delete job is called.
    verify(client, times(1)).sendAsync(eq(deleteRequest), any());
  }

  @Test
  public void testAbortJob() throws JsonProcessingException {
    apiClient = createClient();
    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);

    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    response.setState(JobState.Failed);
    HttpResponse<Object> mockQueryResponse = mockResponse(mapper.writeValueAsString(response), 200);

    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT * FROM ACCOUNT").get();

    apiClient.abortJob(jobId).join();
    String abortPayload = new ObjectMapper().writeValueAsString(new AbortJob());
    // Needs to be updated to initialise the access token correctly.
    var abortRequest =
        HttpRequest.newBuilder(
                URI.create(
                    TEST_SALESFORCE_URI
                        + String.format(
                            io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient
                                .queryJobByIdUri,
                            SALESFORCE_API_VERSION,
                            jobId)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + BEARER_TOKEN)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(abortPayload))
            .build();

    // No 401's so this does not get called.
    verify(login, times(1)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Submit job and delete job.
    verify(client, times(2)).sendAsync(any(HttpRequest.class), any());

    // specifically ensure delete job is called.
    verify(client, times(1)).sendAsync(eq(abortRequest), any());
  }

  @ParameterizedTest
  @EnumSource(JobState.class)
  public void testQueryJobStatus(JobState state) throws JsonProcessingException {
    apiClient = createClient();

    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);

    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    QueryResponse checkResponse = new QueryResponse();
    checkResponse.setId(TEST_JOB_ID);
    checkResponse.setObject("Account");
    checkResponse.setState(state);
    HttpResponse<Object> mockCheckQueryResponse =
        mockResponse(mapper.writeValueAsString(checkResponse), 200);
    HttpResponse<Object> mockQuerySubmitResponse =
        mockResponse(mapper.writeValueAsString(response), 200);
    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockQuerySubmitResponse))
        .thenReturn(CompletableFuture.completedFuture(mockCheckQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT * FROM ACCOUNT").get();
    QueryResponse queryResp = apiClient.queryJobStatus(jobId).get();
    JobState jobStatus = queryResp.getState();
    assertEquals(state, jobStatus);
    // No 401's so this does not get called.

    verify(login, times(1)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Submit job and delete job.
    verify(client, times(2)).sendAsync(any(HttpRequest.class), any());

    var jobStatusRequest =
        HttpRequest.newBuilder(
                URI.create(
                    TEST_SALESFORCE_URI
                        + String.format(
                            io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient
                                .queryJobByIdUri,
                            SALESFORCE_API_VERSION,
                            jobId)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + BEARER_TOKEN)
            .GET()
            .build();

    // specifically ensure delete job is called.
    verify(client, times(1)).sendAsync(eq(jobStatusRequest), any());
  }

  public void testGetResults(JobState state) throws JsonProcessingException {
    apiClient = createClient();

    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);

    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    QueryResponse checkResponse = new QueryResponse();
    checkResponse.setId(TEST_JOB_ID);
    checkResponse.setObject("Account");
    checkResponse.setState(state);
    HttpResponse<Object> mockCheckQueryResponse =
        mockResponse(mapper.writeValueAsString(checkResponse), 200);
    HttpResponse<Object> mockQuerySubmitResponse =
        mockResponse(mapper.writeValueAsString(response), 200);
    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockQuerySubmitResponse))
        .thenReturn(CompletableFuture.completedFuture(mockCheckQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT * FROM ACCOUNT").get();
    QueryResponse queryResp = apiClient.queryJobStatus(jobId).get();
    JobState jobStatus = queryResp.getState();
    assertEquals(state, jobStatus);
    // No 401's so this does not get called.

    verify(login, times(1)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Submit job and delete job.
    verify(client, times(2)).sendAsync(any(HttpRequest.class), any());

    var jobStatusRequest =
        HttpRequest.newBuilder(
                URI.create(
                    TEST_SALESFORCE_URI
                        + String.format(
                            io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient
                                .queryJobByIdUri,
                            SALESFORCE_API_VERSION,
                            jobId)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + BEARER_TOKEN)
            .GET()
            .build();

    // specifically ensure delete job is called.
    verify(client, times(1)).sendAsync(eq(jobStatusRequest), any());
  }

  /**
   * Tests that waitForJob breaks out of the loop after max consecutive failures when getJobStatus
   * consistently returns empty.
   */
  @Test
  public void testWaitForJobMaxRetries() throws JsonProcessingException {
    apiClient = createClient();

    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);

    // Create a job response that is still executing
    QueryResponse initialResponse = new QueryResponse();
    initialResponse.setId(TEST_JOB_ID);
    initialResponse.setState(JobState.InProgress);

    // Mock getJobStatus to consistently return empty (simulating API failures)
    HttpResponse<Object> mockFailureResponse = mockResponse("", 500);
    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockFailureResponse));

    // This should break after some retries.
    QueryResponse result = apiClient.waitForJob(initialResponse, BulkApiClient.ingestJobByIdUri);

    // Should return the initial job info since no successful status checks occurred
    assertThat(result.getId()).isEqualTo(TEST_JOB_ID);
    assertThat(result.getState()).isEqualTo(JobState.InProgress);

    // Verify it attempted the configured number of retries (default is 3 based on max retries)
    verify(client, times(3)).sendAsync(any(HttpRequest.class), any());
  }

  /**
   * Tests that waitForJob resets the consecutive failure counter after a successful status check.
   */
  @Test
  public void testWaitForJobResetsFailureCounter() throws JsonProcessingException {
    apiClient = createClient();

    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(BEARER_TOKEN);

    // Create initial job response
    QueryResponse initialResponse = new QueryResponse();
    initialResponse.setId(TEST_JOB_ID);
    initialResponse.setState(JobState.InProgress);

    // Create successful response showing job still in progress
    QueryResponse progressResponse = new QueryResponse();
    progressResponse.setId(TEST_JOB_ID);
    progressResponse.setState(JobState.InProgress);

    // Create final completed response
    QueryResponse completedResponse = new QueryResponse();
    completedResponse.setId(TEST_JOB_ID);
    completedResponse.setState(JobState.JobComplete);

    HttpResponse<Object> mockFailureResponse = mockResponse("", 500);
    HttpResponse<Object> mockProgressResponse =
        mockResponse(mapper.writeValueAsString(progressResponse), 200);
    HttpResponse<Object> mockCompletedResponse =
        mockResponse(mapper.writeValueAsString(completedResponse), 200);

    // Fail twice, succeed once (which resets counter), fail twice more, then succeed to complete
    when(client.sendAsync(any(HttpRequest.class), any()))
        .thenReturn(CompletableFuture.completedFuture(mockFailureResponse))
        .thenReturn(CompletableFuture.completedFuture(mockFailureResponse))
        .thenReturn(CompletableFuture.completedFuture(mockProgressResponse))
        .thenReturn(CompletableFuture.completedFuture(mockFailureResponse))
        .thenReturn(CompletableFuture.completedFuture(mockFailureResponse))
        .thenReturn(CompletableFuture.completedFuture(mockCompletedResponse));

    // Call waitForJob
    QueryResponse result = apiClient.waitForJob(initialResponse, BulkApiClient.ingestJobByIdUri);

    // Should complete successfully because counter was reset
    assertThat(result.getId()).isEqualTo(TEST_JOB_ID);
    assertThat(result.getState()).isEqualTo(JobState.JobComplete);

    // Should have made all 6 attempts
    verify(client, times(6)).sendAsync(any(HttpRequest.class), any());
  }

  private HttpResponse<Object> mockResponse(String payload, int statusCode) {
    HttpResponse<Object> mockHttpResponse = Mockito.mock(HttpResponse.class);
    when(mockHttpResponse.body()).thenReturn(payload);
    when(mockHttpResponse.statusCode()).thenReturn(statusCode);
    return mockHttpResponse;
  }
}
