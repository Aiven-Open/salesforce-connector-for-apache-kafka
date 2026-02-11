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
package io.aiven.kafka.connect.salesforce.common.bulk.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * QueryResponse Contains all the information that is returned from the
 * Salesforce Bulk API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryResponse {

	private String id;
	private String operation;
	private String object;
	private String createdById;
	private String createdDate;
	private String systemModstamp;
	private JobState state;
	private String concurrencyMode;
	private String contentType;
	private String apiVersion;
	private String lineEnding;
	private String columnDelimiter;

	/**
	 * Default constructor
	 */
	public QueryResponse() {

	}

	/**
	 * Get the job Id
	 * 
	 * @return Id
	 */
	public String getId() {
		return id;
	}
	/**
	 * Set the job Id
	 *
	 * @param id
	 *            The job Id
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * Get the operation
	 * 
	 * @return operation
	 */
	public String getOperation() {
		return operation;
	}
	/**
	 * Set the operation
	 * 
	 * @param operation
	 *            The operation type normally query
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}
	/**
	 * Get the Object
	 * 
	 * @return Object
	 */
	public String getObject() {
		return object;
	}
	/**
	 * Set the Object
	 * 
	 * @param object
	 *            a Salesforce object may be attached in some cases
	 */
	public void setObject(String object) {
		this.object = object;
	}
	/**
	 * Get the createdById
	 * 
	 * @return createdById
	 */
	public String getCreatedById() {
		return createdById;
	}
	/**
	 * Set the createdById
	 * 
	 * @param createdById
	 *            The id of the person who created the operation
	 */
	public void setCreatedById(String createdById) {
		this.createdById = createdById;
	}
	/**
	 * Get the createdDate
	 * 
	 * @return createdDate of when the operation was created
	 */
	public String getCreatedDate() {
		return createdDate;
	}
	/**
	 * Set the createdDate
	 * 
	 * @param createdDate
	 *            of when the operation was created
	 */
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}
	/**
	 * Get the systemModstamp
	 * 
	 * @return systemModstamp
	 */
	public String getSystemModstamp() {
		return systemModstamp;
	}
	/**
	 * Set the systemModstamp
	 * 
	 * @param systemModstamp
	 *            A modification stamp from the system
	 */
	public void setSystemModstamp(String systemModstamp) {
		this.systemModstamp = systemModstamp;
	}
	/**
	 * Get the state
	 * 
	 * @return state
	 */
	public JobState getState() {
		return state;
	}
	/**
	 * Set the state
	 * 
	 * @param state
	 *            The current State of the Job
	 */
	public void setState(JobState state) {
		this.state = state;
	}
	/**
	 * Get the concurrencyMode
	 * 
	 * @return concurrencyMode
	 */
	public String getConcurrencyMode() {
		return concurrencyMode;
	}
	/**
	 * Set the concurrencyMode
	 * 
	 * @param concurrencyMode
	 *            The concurrencyMode selected
	 */
	public void setConcurrencyMode(String concurrencyMode) {
		this.concurrencyMode = concurrencyMode;
	}
	/**
	 * Get the contentType
	 * 
	 * @return contentType The contentType of the query
	 */
	public String getContentType() {
		return contentType;
	}
	/**
	 * Set the contentType
	 * 
	 * @param contentType
	 *            The contentType of the query
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	/**
	 * Get the Api Version
	 * 
	 * @return apiVersion
	 */
	public String getApiVersion() {
		return apiVersion;
	}

	/**
	 * Set the Api Version
	 * 
	 * @param apiVersion
	 *            The version of the api used to submit the query
	 */
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
	/**
	 * Get the line ending
	 * 
	 * @return lineEnding
	 */
	public String getLineEnding() {
		return lineEnding;
	}

	/**
	 * Set the line ending
	 * 
	 * @param lineEnding
	 *            The line ending
	 */
	public void setLineEnding(String lineEnding) {
		this.lineEnding = lineEnding;
	}

	/**
	 * The column delimiter
	 * 
	 * @return The column delimiter
	 */
	public String getColumnDelimiter() {
		return columnDelimiter;
	}

	/**
	 * Set the column delimiter
	 * 
	 * @param columnDelimiter
	 *            The column delimiter
	 */
	public void setColumnDelimiter(String columnDelimiter) {
		this.columnDelimiter = columnDelimiter;
	}
}
