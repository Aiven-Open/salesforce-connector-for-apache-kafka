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
import java.util.stream.Stream;

/**
 * This is a holder for the response from the Bulk Api It allows the storage and
 * processing of the CSV file response.
 */
public class BulkApiResult {

	/**
	 * An object in Salesforce is the table name, thie ObjectName is the object name
	 * from the query submitted to the Salesforce bulk api.
	 */
	private String objectName;
	/**
	 * The first line of the CSV should contain the headers for the CSV
	 */
	private Stream<CSVRecord> contents;

	/**
	 * This constructor allows you to create the object directly from the response
	 * received from the API
	 * 
	 * @param csvString
	 *            this is a String representation of a csv file downloaded from the
	 *            API
	 * @param objectName
	 *            the name of the object that the results came from
	 * @throws IOException
	 *             An IOException can be thrown on creating a csv file from the
	 *             returned query
	 */
	public BulkApiResult(String csvString, String objectName) throws IOException {
		this.contents = CSVFormat.RFC4180.builder().setHeader().get().parse(new StringReader(csvString)).stream();
		this.objectName = objectName;

		// Add additional processing setup to pull
		// out the header files
	}

	/**
	 * This is to retrieve the csv file contents
	 * 
	 * @return The contents of the CSV file
	 */
	public Stream<CSVRecord> getContents() {
		return contents;
	}

	/**
	 * The contents are the contents of the response supplied by the Bulk Api
	 * 
	 * @param contents
	 *            Set the contents for the bulk api result set
	 */
	public void setContents(Stream<CSVRecord> contents) {
		this.contents = contents;
	}

	/**
	 * Get the name of the object these results are from
	 * 
	 * @return the object name
	 */
	public String getObjectName() {
		return objectName;
	}

	/**
	 * Set the ObjectName these queries are from
	 * 
	 * @param objectName
	 *            the object name
	 */
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

}
