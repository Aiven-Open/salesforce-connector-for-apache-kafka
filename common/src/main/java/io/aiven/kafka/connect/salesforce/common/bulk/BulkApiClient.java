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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.commons.util.strings.HttpStatus;
import io.aiven.commons.util.strings.HttpStrings;
import io.aiven.commons.util.timing.AbortTrigger;
import io.aiven.commons.util.timing.Backoff;
import io.aiven.commons.util.timing.BackoffConfig;
import io.aiven.commons.util.timing.SupplierOfLong;
import io.aiven.kafka.connect.salesforce.common.auth.credentials.Oauth2Login;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiQuery;
import io.aiven.kafka.connect.salesforce.common.bulk.query.AbortJob;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResult;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResultResponse;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfig;
import io.aiven.kafka.connect.salesforce.common.exceptions.SFAuthException;
import io.aiven.kafka.connect.salesforce.common.exceptions.SFForbiddenException;
import io.aiven.kafka.connect.salesforce.common.exceptions.SFRetryException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a client for communicating with the Salesforce Bulk Api 2.0 It allows the authentication
 * and creation of jobs, review of their status, return of the data and delete and abort when needed
 * for the Bulk Query API.
 */
public class BulkApiClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(BulkApiClient.class);

  /**
   * The type of operation that is to be made against the Bulk API At the moment only Query
   * operations are supported later this may become an enum to also allow updates
   */
  public static final String QUERY_OPERATION = "query";

  /** Authentication Bearer token identifier to be used with the access_token in http requests */
  public static final String BEARER = "Bearer ";

  /** Header to accept particular content */
  public static final String ACCEPT = "Accept";

  /** A static constant for content-type when expecting a csv to be returned */
  public static final String TEXT_CSV = "text/csv";

  /** A constant for when no query params are being added to a URI */
  public static final String EMPTY_QUERY_PARAM = "";

  /** This is the header name of the locator which is used for pagination in the bulk api */
  private static final String SFORCE_LOCATOR = "Sforce-Locator";

  /** This is the header that tells you the number of records in a response */
  private static final String SFORCE_NUMBER_OF_RECORDS = "Sforce-NumberOfRecords";

  /** This is the header that tells you the current state of the api limit */
  private static final String SFORCE_LIMIT_INFO = "Sforce-Limit-Info";

  /** The Http client that is used to make http requests to Salesforce */
  private final HttpClient client;

  /** This is the URI endpoint which when added to the salesforce uri is used to submit a query. */
  protected static final String submitJobUri = "/services/data/%s/jobs/query";

  /**
   * This is the URI endpoint which when added to the salesforce uri is used to check the status of
   * a query, abort a query or delete a query.
   */
  protected static final String queryJobByIdUri = "/services/data/%s/jobs/query/%s";

  /**
   * This is the URI endpoint which when added to the salesforce uri is used to get the job results
   * of a query.
   */
  protected static final String getJobResultsUri = "/services/data/%s/jobs/query/%s/results";

  // When retrieving results you can add maxRecords to specify the maximum number
  // of records to be returned at a time.
  // Larger queries of data can mean that a timeout may be returned before
  // receiving all the data from Salesforce
  private final String maxRecordsQueryParam =
      "maxRecords="; // NOPMD Not yet used but will be required
  // This String identifies a specific set of results and is used for pagination
  // Not including this parameter will return the first set of results every time
  private final String locatorQueryParam = "locator="; // NOPMD Not yet used but will be required
  private final Oauth2Login login;
  private final ObjectMapper mapper = new ObjectMapper();
  private String accessToken;
  private final SalesforceCommonConfig config;

  /**
   * Constructor
   *
   * @param config SalesforceCommonConfig for this client
   */
  public BulkApiClient(SalesforceCommonConfig config) {
    this(config, HttpClient.newBuilder().build());
  }

  /**
   * Constructor
   *
   * @param config SalesforceCommonConfig for this client
   * @param client A HttpClient for initializing the BulkApiClient
   */
  BulkApiClient(SalesforceCommonConfig config, HttpClient client) {
    this(config, client, new Oauth2Login(config.getSalesforceOauthUri(), client));
  }

  /**
   * Constructor
   *
   * @param config SalesforceCommonConfig for this client
   * @param client A HttpClient for initializing the BulkApiClient
   * @param login The Oauth2Login used to authenticate against the Salesforce api and return a
   *     usable token
   */
  BulkApiClient(SalesforceCommonConfig config, HttpClient client, Oauth2Login login) {
    this.config = config;
    this.client = client;
    this.login = login;
  }

  private Optional<QueryResponse> parseJsonResponse(ExecutionResult executionResult) {
    if (executionResult.hasException()) {
      LOGGER.error(
          "Unable to complete request: {}",
          executionResult.exception.getMessage(),
          executionResult.exception);
    } else {
      try {
        return Optional.of(
            mapper.readValue(executionResult.response().body(), QueryResponse.class));
      } catch (JsonProcessingException e) {
        LOGGER.error("Unable to parse response: {}", e.getMessage(), e);
      }
    }
    return Optional.empty();
  }

  /**
   * Submits a query to the Salesforce Bulk Api v2 throws an exception if unable to submit the query
   * to salesforce
   *
   * @param query Query written in SOQL to submit for bulk query
   * @return Query Job Id
   */
  public Optional<String> submitQueryJob(String query) {
    try {
      String queryObject = mapper.writeValueAsString(new BulkApiQuery(QUERY_OPERATION, query));
      HttpRequest.Builder request =
          HttpRequest.newBuilder(
                  getUriFrom(
                      config.getSalesforceUri() + submitJobUri,
                      EMPTY_QUERY_PARAM,
                      config.getSalesforceApiVersion()))
              .POST(HttpRequest.BodyPublishers.ofString(queryObject));

      return executeHttpRequest(request)
          .thenApply(this::parseJsonResponse)
          .join()
          .map(QueryResponse::getId);
    } catch (JsonProcessingException e) {
      LOGGER.error("Unable to create query: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Polls the job status and returns a QueryResponse if the query was successfull. If not
   * successful, an empty Optional is returned.
   *
   * @param jobId The unique id of the job that is being queried
   * @return a QueryResponse if the query was successfull, an empty Optional if not.
   */
  public Optional<QueryResponse> queryJobStatus(String jobId) {
    try {
      HttpRequest.Builder request =
          HttpRequest.newBuilder(
                  getUriFrom(
                      config.getSalesforceUri() + queryJobByIdUri,
                      EMPTY_QUERY_PARAM,
                      config.getSalesforceApiVersion(),
                      jobId))
              .GET();
      return executeHttpRequest(request).thenApply(this::parseJsonResponse).join();
    } catch (CancellationException | CompletionException e) {
      LOGGER.error("Query submission failed: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Create a BUlkApiResulResponse from an ExecutionResponse.
   *
   * @param executionResult the ExecutionResponse to process.
   * @param objectName the ObjectName that we queried.
   * @param bulkApiKey the BulkApiKey that was used in the query.
   * @return an n Optional bulkApiResultResponse if there was no exception.
   */
  BulkApiResultResponse createResponse(
      final ExecutionResult executionResult, final String objectName, final BulkApiKey bulkApiKey) {

    if (executionResult.hasException()) {
      LOGGER.error(
          "Unable to complete request: {}",
          executionResult.exception.getMessage(),
          executionResult.exception);
      return null;
    } else {
      HttpResponse<String> response = executionResult.response;
      BulkApiResultResponse resp = new BulkApiResultResponse();
      // TODO this is setting the locator as "null" when it does not exist, needs to
      // be understood and fixed
      resp.setLocator(
          response.headers().firstValue(SFORCE_LOCATOR).isPresent()
              ? response.headers().firstValue(SFORCE_LOCATOR).get()
              : null);
      resp.setNumberOfRecords(
          Integer.parseInt(response.headers().firstValue(SFORCE_NUMBER_OF_RECORDS).orElse("-1")));
      resp.setApiUsage(response.headers().firstValue(SFORCE_LIMIT_INFO).orElse("Unknown"));
      resp.setResult(new BulkApiResult(new NativeInfo<>(bulkApiKey, response.body()), objectName));
      LOGGER.info("Current Salesforce API allocation usage: {}", resp.getApiUsage());
      return resp;
    }
  }

  /**
   * Checks if a job is ready to have its results retrieved.
   *
   * @param jobId The unique id of the job that is being queried
   * @param locator The locator is used for pagination in the salesforce bulk API an identifier
   *     returned in the first result set.
   * @param objectName The objectName the query results are coming from
   * @param bulkApiKey The native key for this query
   * @return BulkApiResponseResult if available, {@code null} if not.
   */
  public CompletableFuture<BulkApiResultResponse> getJobResults(
      final String jobId,
      final String locator,
      final String objectName,
      final BulkApiKey bulkApiKey) {
    // This needs to be able to handle multiple pages
    HttpRequest.Builder request =
        HttpRequest.newBuilder(
                buildResultUri(
                    config.getSalesforceUri(), jobId, locator, config.getSalesforceMaxRecords()))
            .header(ACCEPT, TEXT_CSV)
            .GET();
    return executeHttpRequest(request)
        .thenApply(executionResult -> this.createResponse(executionResult, objectName, bulkApiKey));
  }

  /**
   * @param hostUri This is the salesforce base Uri
   * @param jobId the id for the job you are trying to retrieve results for
   * @param locator The locator if there is an additional page to be checked
   * @param maxRecords Ask the api to return a maximum number of records at a time via this value
   * @return A URI to query for results
   */
  private URI buildResultUri(String hostUri, String jobId, String locator, int maxRecords) {
    String queryParams = EMPTY_QUERY_PARAM;
    if (StringUtils.isNotBlank(locator) && !locator.equals("null")) {
      queryParams += (locatorQueryParam + locator);
    }
    if (maxRecords > -1) {
      if (queryParams.contains(locatorQueryParam)) {
        queryParams += "&";
      }
      queryParams += (maxRecordsQueryParam + maxRecords);
    }

    return getUriFrom(
        hostUri + getJobResultsUri, queryParams, config.getSalesforceApiVersion(), jobId);
  }

  /**
   * Attempts to delete the job. Reports success as DEBUG log entry and failure as WARN log entry.
   *
   * @param jobId The unique id of the job that is being deleted
   * @return a future to test when deletion is complete
   */
  public CompletableFuture<Void> deleteJob(String jobId) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(
                getUriFrom(
                    config.getSalesforceUri() + queryJobByIdUri,
                    EMPTY_QUERY_PARAM,
                    config.getSalesforceApiVersion(),
                    jobId))
            .DELETE();
    return executeHttpRequest(request)
        .thenAccept(
            executionResult -> {
              if (executionResult.isSuccess()) {
                LOGGER.debug("JobId {} deleted", jobId);
              } else {
                LOGGER.warn("Deletion of JobId {} failed: {}", jobId, executionResult.getMessage());
              }
            });
  }

  /**
   * Abort an existing job it must be in a JobState of UploadComplete or InProgress to abort
   *
   * @param jobId The unique id of the job that is being queried.
   * @return A future to detect when job is aborted..
   */
  public CompletableFuture<Void> abortJob(String jobId) {
    try {
      String abortPayload = mapper.writeValueAsString(new AbortJob());
      HttpRequest.Builder request =
          HttpRequest.newBuilder(
                  getUriFrom(
                      config.getSalesforceUri() + queryJobByIdUri,
                      EMPTY_QUERY_PARAM,
                      config.getSalesforceApiVersion(),
                      jobId))
              .method("PATCH", HttpRequest.BodyPublishers.ofString(abortPayload));
      return executeHttpRequest(request)
          .thenAccept(
              executionResult -> {
                if (executionResult.isSuccess()) {
                  LOGGER.debug("JobId {} aborted", jobId);
                } else {
                  LOGGER.warn("Abort of JobId {} failed: {}", jobId, executionResult.getMessage());
                }
              });
    } catch (JsonProcessingException e) {
      LOGGER.error("Could not create abort payload {}", e.getMessage(), e);
      return CompletableFuture.failedFuture(e);
    }
  }

  /**
   * The result of an execution request to Salesforce.
   *
   * @param response the response if there was no exception.
   * @param exception the throwable if there was an error.
   */
  public record ExecutionResult(HttpResponse<String> response, Throwable exception) {
    /**
     * Returns {@code trye} if this result has an exception,{@code false} otherwise.
     *
     * @return {@code trye} if this result has an exception,{@code false} otherwise.
     */
    boolean hasException() {
      return exception != null;
    }

    /**
     * Check if a supplied status code is in the [200 - 299] range.
     *
     * @return {@code true} if success, {@code false} if a failure
     */
    boolean isSuccess() {
      return response != null && response.statusCode() >= 200 && response.statusCode() <= 299;
    }

    /**
     * Check if a supplied status code is an authentication error
     *
     * @return {@code true} if success, {@code false} if a failure
     */
    boolean isUnauthorized() {
      return response != null && response.statusCode() == HttpStatus.SC_UNAUTHORIZED;
    }

    /**
     * Check if a supplied status code is an authorization (forbidden) error
     *
     * @return {@code true} if success, {@code false} if a failure
     */
    boolean isForbidden() {
      return response != null && response.statusCode() == HttpStatus.SC_FORBIDDEN;
    }

    /**
     * Checks if the supplied status code is a cuser data error.
     *
     * @return true if the error code is in the [400 - 499] range.
     */
    boolean isClientError() {
      return response != null && response.statusCode() >= 400 && response.statusCode() <= 499;
    }

    /**
     * Checks if the supplied status code is a cuser data error.
     *
     * @return true if the error code is in the [500 - 599] range.
     */
    boolean isServerError() {
      return response != null && response.statusCode() >= 500 && response.statusCode() <= 599;
    }

    /**
     * Gets the message associated with the result state. If an exception was thrown this method
     * will return the execution message. Otherwise, this method returns the text associated with
     * the response status code.
     *
     * @return a message string.
     */
    public String getMessage() {
      if (hasException()) {
        return exception.getMessage();
      }
      return HttpStrings.getReason(response.statusCode());
    }
  }

  /**
   * Handles the respons from the Salesforce query. If there was an exception the ExecutionResult is
   * immediately returned. If the query was a success the ExecutionResult is immediately returned.
   * In all other cases the Execution result is retried until either of the above cases is met or
   * the maximum number of attempts is reached.
   *
   * @param response the response if the query was successfull, otherwise {@code null}.
   * @param exception the exception if the query was not successfull, otherwise {@code null}.
   * @return an ExecutionResult.
   */
  private ExecutionResult responseHandler(HttpResponse<String> response, Throwable exception) {

    ExecutionResult result = new ExecutionResult(response, exception);
    Duration maxBackoffTime = Duration.ofMinutes(3);
    int attempt = 0;

    final Backoff backoff =
        new Backoff(
            new BackoffConfig() {
              @Override
              public SupplierOfLong getSupplierOfTimeRemaining() {
                return maxBackoffTime::toMillis;
              }

              @Override
              public AbortTrigger getAbortTrigger() {
                return null;
              }

              @Override
              public boolean applyTimerRule() {
                return false;
              }
            });

    // TODO should have a list of retryable errors and only retry them.
    // see
    while (attempt <= config.getSalesforceMaxRetries()) {
      if (result.exception() != null) {
        return result;
      }

      if (result.isSuccess()) {
        return result;
      }

      LOGGER.debug(
          "Unsuccessful attempt to query bulk api, attempt number: {}, response code: {}, response message: {}",
          ++attempt,
          result.response().statusCode(),
          result.response().body());

      if (result.isUnauthorized()) {
        try {
          authenticate();
        } catch (SFAuthException e) {
          return new ExecutionResult(null, e);
        }
      } else if (result.isForbidden()) {
        return new ExecutionResult(
            null,
            new SFForbiddenException(
                String.format(
                    "Forbidden from accessing this URI : %s", result.response().request().uri())));
      }

      backoff.cleanDelay();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("response.body() {}", response.body());
      }
      result =
          client
              .sendAsync(response.request(), HttpResponse.BodyHandlers.ofString())
              .handle(ExecutionResult::new)
              .join();
    }

    return new ExecutionResult(
        null,
        new SFRetryException(
            String.format(
                "Could not complete request after %s retries", config.getSalesforceMaxRetries())));
  }

  /**
   * This method executes Http requests to the bulk api and handles retries and authorization
   *
   * @param request The request to be executed against the Bulk Api 2.X
   * @return The response from the request made to the API
   */
  @VisibleForTesting
  CompletableFuture<ExecutionResult> executeHttpRequest(HttpRequest.Builder request) {
    try {
      return client
          .sendAsync(
              request
                  .header("Authorization", BEARER + getAccessToken())
                  .header("Content-Type", "application/json")
                  .build(),
              HttpResponse.BodyHandlers.ofString())
          .handle(this::responseHandler);
    } catch (SFAuthException e) {
      return CompletableFuture.completedFuture(new ExecutionResult(null, e));
    }
  }

  /**
   * @param uri The base which is used to build the URI e.g. https://my.domain.salesforce.com
   * @param queryParams Any additional Query params that should be added at the end of a url, should
   *     not include the '?' identifier and should be in the format of "name=value&name2=value2"
   * @param pathNameParts All the parts that are required to fill the path in the url
   * @return THe uri that makes the specified request.
   */
  private URI getUriFrom(String uri, String queryParams, String... pathNameParts) {
    queryParams = "?" + queryParams;
    return URI.create(
        String.format(uri, (Object[]) pathNameParts)
            + (queryParams.length() > 1 ? queryParams : EMPTY_QUERY_PARAM));
  }

  /**
   * Authenticate with Salesforce will throw an error on failure to authenticate
   *
   * @throws SFAuthException on authentication error.
   */
  public void authenticate() throws SFAuthException {
    accessToken = login.getAccessToken(config.getOauthClientId(), config.getOauthClientSecret());
    if (accessToken == null) {
      throw new SFAuthException(
          "Unable to authenticate with Salesforce please review your configuration settings and try again.");
    }
  }

  /**
   * Get the access token.
   *
   * @throws SFAuthException on authentication error.
   */
  private String getAccessToken() throws SFAuthException {
    if (accessToken == null) {
      authenticate();
    }
    return accessToken;
  }
}
