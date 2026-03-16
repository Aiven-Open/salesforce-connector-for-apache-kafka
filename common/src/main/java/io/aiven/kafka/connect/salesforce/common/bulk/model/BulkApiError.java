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
package io.aiven.kafka.connect.salesforce.common.bulk.model;

/**
 * A POJO to manage the return of exceptions and errors from the Bulk API
 */
public final class BulkApiError {
	private String errorCode;
	private String message;

	/**
	 * BulkApiError default constructor
	 */
	public BulkApiError() {
		// no code
	}
	/**
	 * Get error code from the API
	 * 
	 * @return Get error code from the API
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Set error code from the API
	 * 
	 * @param errorCode
	 *            The error code from the API
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Get the message
	 * 
	 * @return The error message returned from the API
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message
	 * 
	 * @param message
	 *            The error message from the api
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
