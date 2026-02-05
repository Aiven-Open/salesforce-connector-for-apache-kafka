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
package io.aiven.kafka.connect.salesforce.common.auth.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This is a POJO for receiving an authentication response from salesforce in
 * the username-password oauth2 flow.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Oauth2Credentials {

	private String id;
	private String issued_at;
	private String signature;
	private String access_token;
	private String token_type;
	// private String token_format;

	/**
	 * Default constructor
	 */
	public Oauth2Credentials() {
	}

	/**
	 * Get the id
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * set the id
	 * 
	 * @param id
	 *            The id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the time that this token was issued at
	 * 
	 * @return the time that this token was issued at
	 *
	 */
	public String getIssued_at() {
		return issued_at;
	}

	/**
	 * Time that the token was issues at
	 * 
	 * @param issued_at
	 *            Time this token was issued at
	 */
	public void setIssued_at(String issued_at) {
		this.issued_at = issued_at;
	}

	/**
	 * Return the signature on the access token
	 * 
	 * @return the signature on the access token
	 *
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Set the signature on the access token
	 * 
	 * @param signature
	 *            The signature on the access token
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * The Access Token
	 * 
	 * @return The access token that can be used for authentication
	 */
	public String getAccess_token() {
		return access_token;
	}

	/**
	 * Set the access Token
	 * 
	 * @param access_token
	 *            An access token that can be used for authentication
	 */
	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	/**
	 * Get the type of token that is being returned
	 * 
	 * @return The type of token that is being returned
	 */
	public String getToken_type() {
		return token_type;
	}

	/**
	 * The type of token that is being returned
	 * 
	 * @param token_type
	 *            The type of token that is being returned
	 */
	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}
}