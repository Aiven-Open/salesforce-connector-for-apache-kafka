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
import io.aiven.kafka.connect.salesforce.model.BulkApiQuery;
import io.aiven.kafka.connect.salesforce.model.BulkApiResult;
import io.aiven.kafka.connect.salesforce.model.BulkApiResultResponse;
import io.aiven.kafka.connect.salesforce.model.QueryResponse;
import org.apache.commons.csv.CSVRecord;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * This is a client for communicating with the Salesforce Bulk Api 2.0 It allows
 * the authentication and creation of jobs, review of their status, return of
 * the data and delete and abort when needed for the Bulk Query API.
 */
public class BulkApiClient {

	/**
	 * The type of operation that is to be made against the Bulk API At the moment
	 * only Query operations are supported later this may become an enum to also
	 * allow updates
	 */
	public static final String QUERY_OPERATION = "query";
	/**
	 * Authentication Bearer token identifier to be used with the access_token in
	 * http requests
	 **/
	public static final String BEARER = "Bearer ";
	/**
	 * Header to accept particular content
	 */
	public static final String ACCEPT = "Accept";
	/**
	 * A static constant for content-type when expecting a csv to be returned
	 */
	public static final String TEXT_CSV = "text/csv";
	/**
	 * A constant for when no query params are being added to a URI
	 */
	public static final String EMPTY_QUERY_PARAM = "";
	/**
	 * The Http client that is used to make http requests to Salesforce
	 */
	private final HttpClient client;
	/**
	 * This is the URI endpoint which when added to the salesforce uri is used to
	 * submit a query.
	 */
	protected final static String submitJobUri = "/services/data/%s/jobs/query";
	/**
	 * This is the URI endpoint which when added to the salesforce uri is used to
	 * check the status of a query, abort a query or delete a query.
	 */
	protected final static String queryJobByIdUri = "/services/data/%s/jobs/query/%s";

	/**
	 * This is the URI endpoint which when added to the salesforce uri is used to
	 * get the job results of a query.
	 *
	 */
	protected final static String getJobResultsUri = "/services/data/%s/jobs/query/%s/results";

	// When retrieving results you can add maxRecords to specify the maixmum number
	// of records to be returned at a time.
	// Larger queries of data can mean that a timeout may be returned before
	// receiving all the data from Salesforce
	private final String maxRecordsQueryParam = "maxRecords="; // NOPMD Not yet used but will be required
	// This String identifies a specific set of results and is used for pagination
	// Not including this parameter will return the first set of results every time
	private final String locatorQueryParam = "locator="; // NOPMD Not yet used but will be required
	private final Oauth2Login login;
	private final ObjectMapper mapper = new ObjectMapper();
	private String accessToken;

	private final SalesforceConfigFragment configFragment;

	/**
	 * The maximum jitter random number. Should be a power of 2 for speed.
	 */
	public static final int MAX_JITTER = 1024;
	/**
	 * To add randomness to the jitter
	 */
	public static final int JITTER_SUBTRAHEND = MAX_JITTER / 2;
	/**
	 * A random number generator to construct jitter.
	 */
	private final Random random = new Random();

	BulkApiClient(SalesforceConfigFragment configFragment) {
		this(configFragment, HttpClient.newBuilder().build());
	}

	BulkApiClient(SalesforceConfigFragment configFragment, HttpClient client) {
		this(configFragment, client, new Oauth2Login(configFragment.getSalesforceOauthUri(), client));

	}

	BulkApiClient(SalesforceConfigFragment configFragment, HttpClient client, Oauth2Login login) {
		this.configFragment = configFragment;
		this.client = client;
		this.login = login;
	}

	/**
	 * Submits a query to the Salesforce Bulk Api v2 throws an exception if unable
	 * to submit the query to salesforce
	 *
	 * @param query
	 *            Query written in SOQL to submit for bulk query
	 * @return Query Job Id
	 */
	public String submitQueryJob(String query) {
		try {
			String bytes = mapper.writeValueAsString(new BulkApiQuery(QUERY_OPERATION, query));
			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + submitJobUri, EMPTY_QUERY_PARAM,
							configFragment.getSalesforceApiVersion()))
					.POST(HttpRequest.BodyPublishers.ofString(bytes));

			HttpResponse<String> response = executeHttpRequest(request, 1);
			if (isSuccessStatusCode(response.statusCode())) {
				QueryResponse queryResponse = getQueryResponseFromJson(response);
				return queryResponse.getId();
			}

			return null;
			// TODO change to return the Job Id
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Checks if a job is ready to have its results retrieved.
	 * 
	 * @param jobId
	 *            The unique id of the job that is being queried
	 * @return true if ready to return results, false if it is still being processed
	 */
	public QueryResponse queryJobStatus(String jobId) {
		try {

			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + queryJobByIdUri, EMPTY_QUERY_PARAM,
							configFragment.getSalesforceApiVersion(), jobId))
					.GET();
			HttpResponse<String> response = executeHttpRequest(request, 1);

			return getQueryResponseFromJson(response);
			// TODO change to return if the Job State is JobComplete
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	private QueryResponse getQueryResponseFromJson(HttpResponse<String> response) {
		return mapper.readValue(response.body(), QueryResponse.class);
	}

	/**
	 * Checks if a job is ready to have its results retrieved.
	 * 
	 * @param jobId
	 *            The unique id of the job that is being queried
	 * @param locator
	 *            The locator is used for pagination in the salesforce bulk API an
	 *            identifier returned in the first result set.
	 * @param objectName
	 *            The objectName the query results are coming from
	 * @return True if ready to return results, False if it is still being processed
	 */
	public BulkApiResultResponse getJobResults(String jobId, String locator, String objectName) {
		try {

			// This needs to be able to handle multiple pages
			HttpRequest.Builder request = HttpRequest.newBuilder(buildResultUri(configFragment.getSalesforceUri(),
					jobId, locator, configFragment.getSalesforceMaxRecords())).header(ACCEPT, TEXT_CSV).GET();
			HttpResponse<String> response = executeHttpRequest(request, 1);
			BulkApiResultResponse resp = new BulkApiResultResponse();
			if (isSuccessStatusCode(response.statusCode())) {
				resp.setLocator(response.request().headers().firstValue("Sforce-Locator"));
				resp.setLocator(response.request().headers().firstValue("Sforce-NumberOfRecords"));
				try {
					resp.setResult(new BulkApiResult(response.body(), objectName));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return resp;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Creates a stream from which we will create an iterator.
	 * 
	 * @param jobId
	 *            the jobId to begin returning results from
	 * @param locator
	 *            the locator for the next set of results required
	 * @param objectName
	 *            the name of the Salesforce object being queried
	 *
	 * @return a stream of BulkApiSourceRecords
	 */
	public Stream<CSVRecord> getResultStream(String jobId, String locator, String objectName) {

		return Stream.iterate(getJobResults(jobId, locator, objectName), Objects::nonNull, response -> {
			// This should be checking if another locator token exists
			if (response.getLocator().isPresent()) {
				return getJobResults(jobId, response.getLocator().get(), objectName);
			} else {
				return null;
			}
		}).flatMap(response -> response.getResult().getContents());

	}

	/**
	 *
	 * @param hostUri
	 *            This is the salesforce base Uri
	 * @param jobId
	 *            the id for the job you are trying to retrieve results for
	 * @param locator
	 *            The locator if there is an additional page to be checked
	 * @param maxRecords
	 *            Ask the api to return a maximum number of records at a time via
	 *            this value
	 * @return A URI to query for results
	 */
	private URI buildResultUri(String hostUri, String jobId, String locator, int maxRecords) {
		String queryParams = EMPTY_QUERY_PARAM;
		if (locator != null) {
			queryParams += ("locator=" + locator);

		}
		if (maxRecords > -1) {
			queryParams += ("maxRecords=" + maxRecords);
		}

		return getUriFrom(hostUri + getJobResultsUri, queryParams, configFragment.getSalesforceApiVersion(), jobId);

	}

	/**
	 * Delete an existing job
	 *
	 * @param jobId
	 *            The unique id of the job that is being queried
	 */
	public void deleteJob(String jobId) {
		try {
			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + queryJobByIdUri, EMPTY_QUERY_PARAM,
							configFragment.getSalesforceApiVersion(), jobId))
					.DELETE();
			HttpResponse<String> response = executeHttpRequest(request, 1);

			response.statusCode();

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Abort an existing job it must be in a JobState of UploadComplete or
	 * InProgress to abort
	 *
	 * @param jobId
	 *            The unique id of the job that is being queried
	 */
	public void abortJob(String jobId) {
		try {
			String abortPayload = mapper.writeValueAsString(new AbortJob());
			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + queryJobByIdUri, EMPTY_QUERY_PARAM,
							configFragment.getSalesforceApiVersion(), jobId))
					.method("PATCH", HttpRequest.BodyPublishers.ofString(abortPayload));
			HttpResponse<String> response = executeHttpRequest(request, 1);

			response.statusCode();

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * This method executes Http requests to the bulk api and handles retries and
	 * authorization
	 * 
	 * @param request
	 *            The request to be executed against the Bulk Api 2.X
	 * @param attempt
	 *            The number of retries that this particular request has made
	 *            previously to execute the http request
	 * @return The response from the request made to the API
	 */
	private HttpResponse<String> executeHttpRequest(HttpRequest.Builder request, int attempt)
			throws InterruptedException, ExecutionException {
		if (attempt > configFragment.getSalesforceMaxRetries()) {
			throw new RuntimeException("Too many retries");
		}
		CompletableFuture<HttpResponse<String>> future = client.sendAsync(request
				.header("Authorization", BEARER + accessToken).header("Content-Type", "application/json").build(),
				HttpResponse.BodyHandlers.ofString());

		HttpResponse<String> response = future.get();

		if (isSuccessStatusCode(response.statusCode())) {
			return response;
		}

		if (isAuthenticationError(response.statusCode())) {
			// attempt to update status code;
			authenticate();
		} else if (isForbiddenError(response.statusCode())) {
			throw new RuntimeException(
					String.format("Forbidden from accessing this URI : %s", response.request().uri()));
		} else {
			Thread.sleep(timeWithJitter(++attempt));
		}

		executeHttpRequest(request, attempt);

		return response;
	}

	/**
	 *
	 * @param uri
	 *            The base which is used to build the URI e.g
	 *            https://my.domain.salesforce.com
	 * @param queryParams
	 *            Any additional Query params that should be added at the end of a
	 *            url, should not include the '?' identifier and should be in the
	 *            format of "name=value&name2=value2"
	 * @param pathNameParts
	 *            All the parts that are required to fill the path in the url
	 * @return
	 */
	private URI getUriFrom(String uri, String queryParams, String... pathNameParts) {
		queryParams = "?" + queryParams;
		return URI.create(
				String.format(uri, pathNameParts) + (queryParams.length() > 1 ? queryParams : EMPTY_QUERY_PARAM));
	}

	/**
	 * Check if a supplied status code is a 2xx success
	 * 
	 * @param statusCode
	 *            HttpStatusCode that is recieved from a http operation
	 * @return Boolean True if success, False if a failure
	 */
	private boolean isSuccessStatusCode(int statusCode) {
		return statusCode >= 200 && statusCode <= 299;
	}

	/**
	 * Check if a supplied status code is an authentication error
	 * 
	 * @param statusCode
	 *            HttpStatusCode that is received from an http operation
	 * @return Boolean True if success, False if a failure
	 */
	private boolean isAuthenticationError(int statusCode) {
		return statusCode == 401;
	}

	/**
	 * Check if a supplied status code is an authorization (forbidden) error
	 * 
	 * @param statusCode
	 *            HttpStatusCode that is received from an http operation
	 * @return Boolean True if success, False if a failure
	 */
	private boolean isForbiddenError(int statusCode) {
		return statusCode == 403;
	}

	/**
	 * Calculate the wait time with Jitter before retrying an operation This should
	 * be replaced with BackOff class from aiven-commons
	 *
	 * @param waitCount
	 *            the count of how many times the operation has been tried already
	 * @return Boolean True if success, False if a failure
	 */
	private long timeWithJitter(int waitCount) {
		// generate approx +/- 0.512 seconds of jitter
		final int jitter = random.nextInt(MAX_JITTER) - JITTER_SUBTRAHEND;
		return (long) Math.abs(Math.pow(2, waitCount) + jitter);
	}

	/**
	 * Authenticate with Salesforce will throw an error on failure to authenticate
	 */
	public void authenticate() {
		accessToken = login.getAccessToken(configFragment.getOauthClientId(), configFragment.getOauthClientSecret());
		if (accessToken == null) {
			throw new RuntimeException(
					"Unable to authenticate with Salesforce please review your configuration settings and try again.");
		}
	}

}
