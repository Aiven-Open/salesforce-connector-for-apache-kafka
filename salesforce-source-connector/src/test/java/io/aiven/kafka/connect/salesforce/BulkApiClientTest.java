/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aiven.kafka.connect.salesforce;

import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.credentials.Oauth2Login;

import io.aiven.kafka.connect.salesforce.model.AbortJob;
import io.aiven.kafka.connect.salesforce.model.JobState;
import io.aiven.kafka.connect.salesforce.model.QueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the Bulk Api Client to confirm it behaves in an expected way.
 */
public class BulkApiClientTest {

	public static final String TEST_CLIENT_ID = "test_client_id";
	public static final String TEST_CLIENT_SECRET = "test_client_secret";
	public static final String TEST_OAUTH_URI = "https://localhost:1234/oauth";
	public static final String TEST_SALESFORCE_URI = "https://localhost:1234/";
	public static final String TEST_ACCESS_TOKEN = "test_access_token";
	public static final String TEST_GRANT_TYPE = "client_credentials";
	public static final String TEST_JOB_ID = "test-job";
	public static final String SALESFORCE_API_VERSION = "v65.0";

	private Oauth2Login login;

	private SalesforceConfigFragment configFragment;

	private HttpClient client;

	@InjectMocks
	private BulkApiClient apiClient;

	ObjectMapper mapper = new ObjectMapper();

	@BeforeEach
	public void setup() {
		configFragment = mock(SalesforceConfigFragment.class);
		login = mock(Oauth2Login.class);
		client = mock(HttpClient.class);
		apiClient = new BulkApiClient(configFragment, client, login);
	}

	@Test
  public void testAutomaticRetryOfCredentials() {

    when(configFragment.getOauthClientId()).thenReturn(TEST_CLIENT_ID);
    when(configFragment.getOauthClientSecret()).thenReturn(TEST_CLIENT_SECRET);
    when(configFragment.getSalesforceOauthUri()).thenReturn(TEST_OAUTH_URI);
    when(configFragment.getSalesforceUri()).thenReturn(TEST_SALESFORCE_URI);
    when(configFragment.getSalesforceMaxRetries()).thenReturn(3);
    apiClient = new BulkApiClient(configFragment, client, login);
    HttpResponse mockQueryResponse = Mockito.mock(HttpResponse.class);
    //return 401 twice as its different checks for success and authentication failure.
    when(mockQueryResponse.statusCode()).thenReturn(401).thenReturn(401).thenReturn(200);
    when(login.getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET))).thenReturn(
        TEST_ACCESS_TOKEN);

    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    when(mockQueryResponse.body()).thenReturn(mapper.writeValueAsString(response));

    when(client.sendAsync(any(HttpRequest.class),any())).thenReturn(CompletableFuture.completedFuture(mockQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT * FROM ACCOUNT");
    assertEquals(TEST_JOB_ID,jobId);
    verify(login,times(1)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Retries and returns successfully the second time
    verify(client,times(2)).sendAsync(any(HttpRequest.class),any());

  }

	@Test
	public void testRetryExpectedNumberOfTimes() {
		int expectedNumberOfRetries = 3;
		when(configFragment.getOauthClientId()).thenReturn(TEST_CLIENT_ID);
		when(configFragment.getOauthClientSecret()).thenReturn(TEST_CLIENT_SECRET);
		when(configFragment.getSalesforceOauthUri()).thenReturn(TEST_OAUTH_URI);
		when(configFragment.getSalesforceUri()).thenReturn(TEST_SALESFORCE_URI);
		when(configFragment.getSalesforceMaxRetries()).thenReturn(expectedNumberOfRetries);
		apiClient = new BulkApiClient(configFragment, client, login);
		HttpResponse<Object> mockQueryResponse = Mockito.mock(HttpResponse.class);
		// return 401 twice as its different checks for success and authentication
		// failure.
		when(mockQueryResponse.statusCode()).thenReturn(500).thenReturn(500).thenReturn(500);

		QueryResponse response = new QueryResponse();
		response.setId(TEST_JOB_ID);
		response.setObject("Account");
		when(mockQueryResponse.body()).thenReturn(mapper.writeValueAsString(response));

		when(client.sendAsync(any(HttpRequest.class), any()))
				.thenReturn(CompletableFuture.completedFuture(mockQueryResponse));

		assertThrowsExactly(RuntimeException.class, () -> {
			apiClient.submitQueryJob("SELECT * FROM ACCOUNT");;
		}, "Too many retries");

		// No 401's so this does not get called.
		verify(login, times(0)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

		// Retries and returns successfully the second time
		verify(client, times(expectedNumberOfRetries)).sendAsync(any(HttpRequest.class), any());

	}

	@Test
  public void testDeleteJob() {

    when(configFragment.getOauthClientId()).thenReturn(TEST_CLIENT_ID);
    when(configFragment.getOauthClientSecret()).thenReturn(TEST_CLIENT_SECRET);
    when(configFragment.getSalesforceOauthUri()).thenReturn(TEST_OAUTH_URI);
    when(configFragment.getSalesforceUri()).thenReturn(TEST_SALESFORCE_URI);
    when(configFragment.getSalesforceApiVersion()).thenReturn(SALESFORCE_API_VERSION);
    when(configFragment.getSalesforceMaxRetries()).thenReturn(3);
    apiClient = new BulkApiClient(configFragment, client, login);

    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    HttpResponse<Object> mockQueryResponse = mockResponse(mapper.writeValueAsString(response), 200);

    when(client.sendAsync(any(HttpRequest.class),any())).thenReturn(CompletableFuture.completedFuture(mockQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT Id, Name FROM ACCOUNT");

    apiClient.deleteJob(jobId);
    //   Needs to be updated to initialise the access token correctly.
    var deleteRequest = HttpRequest.newBuilder(URI.create(TEST_SALESFORCE_URI + String.format(BulkApiClient.queryJobByIdUri, SALESFORCE_API_VERSION, jobId))).header("Content-Type", "application/json").header("Authorization", "Bearer null").DELETE().build();

    // No 401's so this does not get called.
    verify(login,times(0)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));


    // Submit job and delete job.
    verify(client,times(2)).sendAsync(any(HttpRequest.class),any());

    //specifically ensure delete job is called.
    verify(client,times(1)).sendAsync(eq(deleteRequest),any());

  }

	@Test
  public void testAbortJob() {

    when(configFragment.getOauthClientId()).thenReturn(TEST_CLIENT_ID);
    when(configFragment.getOauthClientSecret()).thenReturn(TEST_CLIENT_SECRET);
    when(configFragment.getSalesforceOauthUri()).thenReturn(TEST_OAUTH_URI);
    when(configFragment.getSalesforceUri()).thenReturn(TEST_SALESFORCE_URI);
    when(configFragment.getSalesforceApiVersion()).thenReturn(SALESFORCE_API_VERSION);
    when(configFragment.getSalesforceMaxRetries()).thenReturn(3);
    apiClient = new BulkApiClient(configFragment, client, login);


    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    HttpResponse<Object> mockQueryResponse = mockResponse(mapper.writeValueAsString(response), 200);

    when(client.sendAsync(any(HttpRequest.class),any())).thenReturn(CompletableFuture.completedFuture(mockQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT * FROM ACCOUNT");

    apiClient.abortJob(jobId);
    String abortPayload = new ObjectMapper().writeValueAsString(new AbortJob());
    //   Needs to be updated to initialise the access token correctly.
    var abortRequest = HttpRequest.newBuilder(URI.create(TEST_SALESFORCE_URI + String.format(BulkApiClient.queryJobByIdUri, SALESFORCE_API_VERSION, jobId))).header("Content-Type", "application/json").header("Authorization", "Bearer null").method("PATCH", HttpRequest.BodyPublishers.ofString(abortPayload)).build();

    // No 401's so this does not get called.
    verify(login,times(0)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));

    // Submit job and delete job.
    verify(client,times(2)).sendAsync(any(HttpRequest.class),any());

    //specifically ensure delete job is called.
    verify(client,times(1)).sendAsync(eq(abortRequest),any());

  }

	@ParameterizedTest
  @EnumSource(JobState.class)
  public void testQueryJobStatus(JobState state) {

    when(configFragment.getOauthClientId()).thenReturn(TEST_CLIENT_ID);
    when(configFragment.getOauthClientSecret()).thenReturn(TEST_CLIENT_SECRET);
    when(configFragment.getSalesforceOauthUri()).thenReturn(TEST_OAUTH_URI);
    when(configFragment.getSalesforceUri()).thenReturn(TEST_SALESFORCE_URI);
    when(configFragment.getSalesforceApiVersion()).thenReturn(SALESFORCE_API_VERSION);
    when(configFragment.getSalesforceMaxRetries()).thenReturn(3);
    apiClient = new BulkApiClient(configFragment, client, login);



    QueryResponse response = new QueryResponse();
    response.setId(TEST_JOB_ID);
    response.setObject("Account");
    QueryResponse checkResponse = new QueryResponse();
    checkResponse.setId(TEST_JOB_ID);
    checkResponse.setObject("Account");
    checkResponse.setState(state);
    HttpResponse<Object> mockCheckQueryResponse = mockResponse(mapper.writeValueAsString(checkResponse), 200);
    HttpResponse<Object> mockQuerySubmitResponse = mockResponse(mapper.writeValueAsString(response),200);
    when(client.sendAsync(any(HttpRequest.class),any())).thenReturn(CompletableFuture.completedFuture(mockQuerySubmitResponse))
                                                        .thenReturn(CompletableFuture.completedFuture(mockCheckQueryResponse));

    String jobId = apiClient.submitQueryJob("SELECT * FROM ACCOUNT");
    var queryResp = apiClient.queryJobStatus(jobId);
    JobState jobStatus = queryResp.getState();
    assertEquals(state, jobStatus);
    // No 401's so this does not get called.

    verify(login,times(0)).getAccessToken(eq(TEST_CLIENT_ID), eq(TEST_CLIENT_SECRET));


    // Submit job and delete job.
    verify(client,times(2)).sendAsync(any(HttpRequest.class),any());


    var jobStatusRequest = HttpRequest.newBuilder(URI.create(TEST_SALESFORCE_URI + String.format(BulkApiClient.queryJobByIdUri, SALESFORCE_API_VERSION, jobId))).header("Content-Type", "application/json").header("Authorization", "Bearer null").GET().build();


    //specifically ensure delete job is called.
    verify(client,times(1)).sendAsync(eq(jobStatusRequest),any());

  }

	private HttpResponse<Object> mockResponse(String payload, int statusCode) {
		HttpResponse<Object> mockHttpResponse = Mockito.mock(HttpResponse.class);
		when(mockHttpResponse.body()).thenReturn(payload);
		when(mockHttpResponse.statusCode()).thenReturn(statusCode);
		return mockHttpResponse;
	}

}
