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
package io.aiven.kafka.connect.salesforce.common.bulk.model;

import io.aiven.commons.kafka.connector.source.task.Context;

/**
 * Extended Context to take in additional context for Salesforce
 */
public class SalesforceContext extends Context {

	private String jobId;
	private int totalRecords;
	private String lastModifiedTimestamp;

	/**
	 * SalesforceContext
	 * 
	 * @param nativeKey
	 *            The native key for the Context
	 */
	public SalesforceContext(Object nativeKey) {
		super(nativeKey);
	}

	/**
	 * Constructor to build a context from another context
	 * 
	 * @param anotherContext
	 *            Another Context
	 */
	protected SalesforceContext(SalesforceContext anotherContext) {
		super(anotherContext);
		this.jobId = anotherContext.jobId;
		this.totalRecords = anotherContext.totalRecords;
		this.lastModifiedTimestamp = anotherContext.getLastModifiedTimestamp();
	}

	/**
	 * Get the Job Id
	 * 
	 * @return the Job Id
	 */
	public String getJobId() {
		return jobId;
	}

	/**
	 * Get the total number of records in the job
	 * 
	 * @return the total number of records in the job
	 */
	public int getTotalRecords() {
		return totalRecords;
	}

	/**
	 * Set the JobId
	 * 
	 * @param jobId
	 *            the JobId
	 */
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	/**
	 * Set the total number of records in the job
	 * 
	 * @param totalRecords
	 *            the total number of records in the job
	 */
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}

	/**
	 * Get lastModifiedTimestamp used in the job to retrieve data
	 * 
	 * @return lastModifiedTimestamp
	 */
	public String getLastModifiedTimestamp() {
		return lastModifiedTimestamp;
	}

	/**
	 * Set the lastModifiedTimestamp used in the job
	 * 
	 * @param lastModifiedTimestamp
	 *            lastModifiedTimestamp used in the job to retrieve data
	 */
	public void setLastModifiedTimestamp(String lastModifiedTimestamp) {
		this.lastModifiedTimestamp = lastModifiedTimestamp;
	}
}
