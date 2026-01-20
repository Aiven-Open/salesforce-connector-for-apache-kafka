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
import io.aiven.kafka.connect.salesforce.model.BulkApiQuery;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
	private final HttpClient client;
	// TODO Not happy with these names revisit
	private final static String submitJobUri = "/services/data/%s/jobs/query";
	private final static String queryJobByIdUri = "/services/data/%s/jobs/query/queryJobId/";
	private final static String getJobResultsUri = "/services/data/%s/jobs/query/results";
	private final static String deleteJobUri = "/services/data/%s/jobs/query/queryJobId/";
	private final static String abortJobUri = "/services/data/%s/jobs/query/queryJobId/";
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
	Random random = new Random();

	BulkApiClient(String baseUri, SalesforceConfigFragment configFragment) {
		this.configFragment = configFragment;
		client = HttpClient.newBuilder().build();
		login = new Oauth2Login(baseUri, client);
	}

	/**
	 * Submits a query to the Salesforce Bulk Api v2 throws an exception if unable
	 * to submit the query to salesforce
	 *
	 * @param query
	 *            Query written in SOQL to submit for bulk query
	 * @return Query Job Id
	 */
	public boolean submitQueryJob(String query) {
		try {
			byte[] bytes = mapper.writeValueAsBytes(new BulkApiQuery(QUERY_OPERATION, query));
			HttpRequest.Builder request = HttpRequest.newBuilder(getUriFrom(submitJobUri))
					.POST(HttpRequest.BodyPublishers.ofByteArray(bytes));
			HttpResponse<String> response = executeHttpRequest(request, 0);
			return isSuccessStatusCode(response.statusCode());
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
	public boolean checkQueryJobStatus(String jobId) {
		try {

			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + queryJobByIdUri,
							configFragment.getSalesforceApiVersion(), jobId))
					.GET();
			HttpResponse<String> response = executeHttpRequest(request, 0);
			return isSuccessStatusCode(response.statusCode());
			// TODO change to return if the Job State is JobComplete
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
	public boolean getJobResults(String jobId) {
		try {

			// This needs to be able to handle multiple pages
			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + getJobResultsUri,
							configFragment.getSalesforceApiVersion(), jobId))
					.GET();
			HttpResponse<String> response = executeHttpRequest(request, 0);

			return isSuccessStatusCode(response.statusCode());
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Delete an existing job
	 * 
	 * @param jobId
	 *            The unique id of the job that is being queried
	 * @return Boolean value indicating success or failure of the operation
	 */
	public boolean deleteJob(String jobId) {
		try {
			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + deleteJobUri,
							configFragment.getSalesforceApiVersion(), jobId))
					.DELETE();
			HttpResponse<String> response = executeHttpRequest(request, 0);

			return isSuccessStatusCode(response.statusCode());

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Abort an existing job
	 * 
	 * @param jobId
	 *            The unique id of the job that is being queried
	 * @return Boolean value indicating success or failure of the operation
	 */
	public boolean abortJob(String jobId) {
		try {
			HttpRequest.Builder request = HttpRequest
					.newBuilder(getUriFrom(configFragment.getSalesforceUri() + abortJobUri,
							configFragment.getSalesforceApiVersion(), jobId))
					.DELETE();
			HttpResponse<String> response = executeHttpRequest(request, 0);

			return isSuccessStatusCode(response.statusCode());

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
		if (attempt >= configFragment.getSalesforceMaxRecords()) {
			throw new RuntimeException("Too many retries");
		}
		CompletableFuture<HttpResponse<String>> future = client.sendAsync(
				request.header("Authorization", BEARER + accessToken).build(), HttpResponse.BodyHandlers.ofString());

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
			Thread.sleep(timeWithJitter(attempt));
		}

		executeHttpRequest(request, ++attempt);

		return response;
	}
	private URI getUriFrom(String uri, String... parts) {

		return URI.create(String.format(uri, parts));
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
		return (long) Math.pow(2, waitCount) + jitter;
	}

	/**
	 * Authenticate with Salesforce will throw an error on failure to authenticate
	 */
	private void authenticate() {
		accessToken = login.getAccessToken("password", configFragment.getOauthClientId(),
				configFragment.getOauthClientSecret(), configFragment.getOauthUsername(),
				configFragment.getOauthPassword());
		if (accessToken == null) {
			throw new RuntimeException(
					"Unable to authenticate with Salesforce please review your configuration settings and try again.");
		}
	}

}
