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

/**
 * Client credentials body for authenticating with salesforce The toString()
 * method provides the appropriately formatted bod to be used with
 * application/www-x-form-urlencoded
 */
public class SalesforceClientCredentials {

	private final static String GRANT_TYPE = "client_credentials";

	private String clientId;

	private String clientSecret;

	/**
	 * The constructor for the client credentials
	 * 
	 * @param clientId
	 *            the client Id used for authentication
	 * @param clientSecret
	 *            the client secret to be used for authentication
	 */
	public SalesforceClientCredentials(String clientId, String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	/**
	 * Returns the client id that is being used for authenticatoin
	 * 
	 * @return the client Id
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * The client id to authenticate with
	 * 
	 * @param clientId
	 *            The client id
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Get the client Secret
	 * 
	 * @return The client secret
	 */
	public String getClientSecret() {
		return clientSecret;
	}

	/**
	 * Setter for the client secret
	 * 
	 * @param clientSecret
	 *            the client secret
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	/**
	 * This method provides the expected format for the client credentials request
	 * that aligns with the application/x-www-form-urlencoded that is expected by
	 * the login api
	 */
	String toFormUrlEncodedFormat() {
		return "grant_type=" + GRANT_TYPE + "&client_id=" + clientId + "&client_secret=" + clientSecret;
	}
}
