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
package io.aiven.kafka.connect.salesforce.model;

import java.util.Optional;
import java.util.OptionalInt;

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
	private Optional<String> locator;

	/**
	 * The maximum number of records returned in this response
	 */
	private OptionalInt numberOfRecords;
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
	 * The number of records that is included in this response
	 * 
	 * @return An OptionalInt which may contain the number of records in this
	 *         response
	 */
	public OptionalInt getNumberOfRecords() {
		return numberOfRecords;
	}

	/**
	 * Set the number of records that are in this response
	 * 
	 * @param numberOfRecords
	 *            Set the OptionalInt number of Records in this response
	 */
	public void setNumberOfRecords(OptionalInt numberOfRecords) {
		this.numberOfRecords = numberOfRecords;
	}

	/**
	 * Get the locator which is used in the retrieval of the next csv file if there
	 * is a next csv file.
	 * 
	 * @return A String which can be used to get the next set of results
	 */
	public Optional<String> getLocator() {
		return locator;
	}

	/**
	 * Set the Locator which is used to identify and retrieve the next set of
	 * results from the bulk api.
	 * 
	 * @param locator
	 *            An Optional String which is used by the Bulk Api to retrieve the
	 *            next CSV file
	 */
	public void setLocator(Optional<String> locator) {
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
}
