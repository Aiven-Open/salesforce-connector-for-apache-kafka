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
package io.aiven.kafka.connect.salesforce.common.bulk.query;

/**
 * BulkApiResultResponse includes the contents of the returned csv as well as
 * the http headers that contain the number of records in the file and also the
 * locator to use in the next request.
 */
public class BulkApiResultResponse {

	/**
	 * The body of the data from the result
	 */
	private BulkApiResult result;
	/**
	 * The locator returned in the response needed for the next query
	 */
	private String locator;

	/**
	 * The maximum number of records returned in this response
	 */
	private int numberOfRecords;

	/**
	 * The API Usage and tracks how much of the api limit has been consumed
	 */
	private String apiUsage;
	/**
	 * Default constructor for the BulkApiResultResponse object
	 */
	public BulkApiResultResponse() {
	}

	/**
	 * Get the BulkApiResul which contains the body of the response This is a csv
	 * file in String format
	 * 
	 * @return A BulkApiResult object
	 */
	public BulkApiResult getResult() {
		return result;
	}

	/**
	 * Get the locator which is used in the retrieval of the next csv file if there
	 * is a next csv file.
	 * 
	 * @return A String which can be used to get the next set of results
	 */
	public String getLocator() {
		return locator;
	}

	/**
	 * Set the Locator which is used to identify and retrieve the next set of
	 * results from the bulk api.
	 * 
	 * @param locator
	 *            A String which is used by the Bulk Api to retrieve the next CSV
	 *            file
	 */
	public void setLocator(String locator) {
		this.locator = locator;
	}

	/**
	 * This is used to retrieve the contents of the body which is a csv file in
	 * string format
	 * 
	 * @param result
	 *            Get the result set which includes the contents of the body
	 */
	public void setResult(BulkApiResult result) {
		this.result = result;
	}

	/**
	 * Get the number of records expected in this result set
	 * 
	 * @return the number of records expected in this result set
	 */
	public int getNumberOfRecords() {
		return numberOfRecords;
	}

	/**
	 * Get the API Usage
	 * 
	 * @return Get the Api Usage
	 */
	public String getApiUsage() {
		return apiUsage;
	}

	/**
	 * Set the Api Usage
	 * 
	 * @param apiUsage
	 *            the api usage
	 */
	public void setApiUsage(String apiUsage) {
		this.apiUsage = apiUsage;
	}

	/**
	 * Set the number of records expected in this result set
	 * 
	 * @param numberOfRecords
	 *            the number of records expected in this result set
	 */
	public void setNumberOfRecords(int numberOfRecords) {
		this.numberOfRecords = numberOfRecords;
	}
}
