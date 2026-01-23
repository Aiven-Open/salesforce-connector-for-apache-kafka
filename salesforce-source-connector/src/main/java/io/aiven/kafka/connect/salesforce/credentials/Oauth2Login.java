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
package io.aiven.kafka.connect.salesforce.credentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This class allows a username and password to login to salesforce and retrieve
 * an access token for authentication.
 */
public class Oauth2Login {
	private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2Login.class);
	private final static String GRANT_TYPE = "grant_type";
	private final static String CLIENT_ID = "client_id";
	private final static String CLIENT_SECRET = "client_secret";
	private final static String USERNAME = "username";
	private final static String PASSWORD = "password";
	private final String URI;
	private final HttpClient client;
	ObjectMapper mapper = new ObjectMapper();

	/**
	 * Constructor for Oauth2Login
	 * 
	 * @param uri
	 *            The Uri to authenticate against
	 * @param client
	 *            The Http Client to use for authentication
	 * @throws IllegalArgumentException
	 *             If the uri is not valid the constructor will throw an
	 *             IllegalArgumentException
	 */
	public Oauth2Login(String uri, HttpClient client) throws IllegalArgumentException {
		this.URI = uri;
		validateUri(URI);
		this.client = client;
	}

	/**
	 * Validate the supplied URI to make sure it is using the proper scheme
	 * 
	 * @param uri
	 */
	private void validateUri(String uri) {
		if (!uri.startsWith("https://")) {
			throw new IllegalArgumentException(
					"Provided URI for Salesforce authentication schema must be secure https://");
		}
	}

	/**
	 * Gets an access token from salesforce for authentication using the username
	 * password oauth2 flow
	 *
	 * @param grantType
	 *            Specifies the type of grant you are seeking typically it is
	 *            "password" - option to set this may be removed before release
	 * @param clientId
	 *            Specifies the clientId that is configured in Salesforce for Oauth
	 *            authentication
	 * @param clientSecret
	 *            Specifies the client Secret that is configured in Salesforce for
	 *            Oauth authentication
	 * @param username
	 *            Specifies the username that is configured in Salesforce for Oauth
	 *            authentication
	 * @param password
	 *            Specifies the password that is configured in Salesforce for Oauth
	 *            authentication
	 * @return Access Token for the Salesforce organization
	 */
	public String getAccessToken(String grantType, String clientId, String clientSecret, String username,
			String password) {

		String parameters = String.format("?%s=%s&%s=%s&%s=%s&%s=%s&%s=%s", GRANT_TYPE, grantType, CLIENT_ID, clientId,
				CLIENT_SECRET, clientSecret, USERNAME, username, PASSWORD, password);
		try {
			URI uri = new URI(URI + parameters);
			HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
			CompletableFuture<HttpResponse<String>> future = client.sendAsync(request,
					HttpResponse.BodyHandlers.ofString());
			HttpResponse<String> response = future.get();
			if (response.statusCode() == 200) {
				Oauth2Credentials credentials = mapper.readValue(response.body(), Oauth2Credentials.class);
				return credentials.getAccess_token();
			} else {
				// TODO improve exception information
				throw new RuntimeException(String.format("Invalid response code received from Salesforce oauth flow %s",
						response.statusCode()));
			}

		} catch (URISyntaxException | ExecutionException | InterruptedException e) {
			LOGGER.error("Exception thrown authenticating with oauth", e);
			throw new RuntimeException(e);
		}

	}

}
