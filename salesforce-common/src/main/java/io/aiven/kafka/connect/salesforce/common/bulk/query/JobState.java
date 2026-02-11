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

/**
 * The State of the Bulk API 2.0 Query Job states as described in the
 * documentation, in addition there is a `Submitted` and a 'fulfilled' state
 * used internally in the connector.
 * https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/bulk_api_2_job_states.htm
 */
public enum JobState {
	/**
	 * A new job has been submitted to the API
	 */
	Submitted("Submitted"),
	/**
	 * The job has been completed and all results have been successfully returned
	 */
	Fulfilled("Fulfilled"),
	/**
	 * An ingest job was created and is open for data uploads.
	 */
	Open("Open"),
	/**
	 * (Ingest) All job data has been uploaded and the job is ready to be processed.
	 *
	 * (Query) The job is ready to be processed.
	 */
	UploadComplete("UploadComplete"),
	/**
	 * The job is being processed by Salesforce. Operations include automatic,
	 * optimized batching or chunking of job data, and processing of job operations.
	 */
	InProgress("InProgress"),
	/**
	 * The job was canceled by the job creator, or by a user with the “Manage Data
	 * Integrations” permission. It should be deleted
	 */
	Aborted("Aborted"),
	/**
	 * The job was processed. It is now able to be queried for the results
	 */
	JobComplete("JobComplete"),
	/**
	 * The job couldn’t be processed successfully. It should be deleted
	 */
	Failed("Failed");

	private final String value;
	JobState(String value) {
		this.value = value;
	}

	/**
	 * Get the String value of the JobState Enum
	 * 
	 * @return the String value of the JobState Enum
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	/**
	 * Using a string value get the enum value of the JobState
	 * 
	 * @param value
	 *            The string version of the Enum value
	 * @return The Enum Value of the JobState
	 */
	public static JobState getByValue(String value) {
		for (JobState state : values()) {
			if (state.value.equals(value)) {
				return state;
			}
		}
		throw new IllegalArgumentException("Unsupported Job State");
	}
}
