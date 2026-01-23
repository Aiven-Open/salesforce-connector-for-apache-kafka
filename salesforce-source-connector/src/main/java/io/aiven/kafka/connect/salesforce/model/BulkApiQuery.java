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

/**
 * A model for submitting a bulk api v2.0 Query job.
 */
public class BulkApiQuery {

	/**
	 * Create a new Bulk Api Query to submit to the API
	 * 
	 * @param operation
	 *            The type of operation to execute against the API currently only
	 *            'query' is allowed
	 * @param query
	 *            The SOQL query to execute against the Bulk API
	 */
	public BulkApiQuery(String operation, String query) {
		this.operation = operation;
		this.query = query;
	}
	/*
	 * default is "query"
	 */
	private String operation;
	/*
	 * Query that uses the Salesforce Object Query Language
	 */
	private String query;

	/**
	 * Get the operation type for this query
	 * 
	 * @return The operation type as a String
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * Set the operation type for the query
	 * 
	 * @param operation
	 *            The operation type currently on query is supported
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	 * Get the SOQL query for the operation
	 * 
	 * @return The SOQL query for the operation
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Set the SOQL query for the operation
	 * 
	 * @param query
	 *            The SOWL Query to execute against the API
	 */
	public void setQuery(String query) {
		this.query = query;
	}
}
