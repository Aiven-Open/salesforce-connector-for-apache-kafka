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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is a holder for the response from the Bulk Api It allows the storage and
 * processing of the CSV file response.
 */
public class BulkApiResult {

	/**
	 * The first line of the CSV should contain the headers for the CSV
	 */
	private List<String> headers;
	/**
	 * The CSV returned from the API sill in String format Need to review size
	 * constraints etc
	 */
	private String contents;

	/**
	 * This constructor allows you to create the object directly from the response
	 * received from the API
	 * 
	 * @param csvString
	 *            this is a String representation of a csv file downloaded from the
	 *            API
	 */
	public BulkApiResult(String csvString) {
		this.contents = csvString;
		// Add additional processing setup to pull
		// out the header files
	}

	/**
	 * The headers are the column names which are on the first line of the CSV file
	 * they match with the contents as you move down
	 * 
	 * @return The headers from this resultSet
	 */
	public List<String> getHeaders() {
		return headers;
	}

	/**
	 * Set the headers for this result set these are the column names which are on
	 * the first line of the CSV file they match with the contents as you move down
	 * 
	 * @param headers
	 *            Set the headers for this resultSet
	 */
	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	/**
	 * This is to retrieve the csv file contents
	 * 
	 * @return The contents of the CSV file
	 */
	public String getContents() {
		return contents;
	}

	/**
	 * The contents are the contents of the response supplied by the Bulk Api
	 * 
	 * @param contents
	 *            Set the contents for the bulk api result set
	 */
	public void setContents(String contents) {
		this.contents = contents;
	}

	/**
	 * This method creates a stream of CSVRecords that can be processed
	 *
	 * @return A stream of CSV Records
	 * @throws IOException
	 *             could be thrown when retrieving a stream of CSV Records
	 */
	public Stream<CSVRecord> getRecords() throws IOException {
		return CSVFormat.RFC4180.builder().setHeader(headers.toArray(String[]::new)).get()
				.parse(new StringReader(contents)).stream();
	}
}
