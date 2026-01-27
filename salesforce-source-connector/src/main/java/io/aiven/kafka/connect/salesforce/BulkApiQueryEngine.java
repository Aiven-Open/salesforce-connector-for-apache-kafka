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
package io.aiven.kafka.connect.salesforce;

import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aiven.kafka.connect.salesforce.model.JobState;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The BulkApiQueryEngine handles taking the config from the connector and
 * making the relevant queries to the Salesforce BulkApi 2.0 It handles the
 * lifecycle of the requests along ith exceptions
 */
public class BulkApiQueryEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(BulkApiQueryEngine.class);
	private SalesforceConfigFragment configFragment; // NOPMD
	private final BulkApiClient apiClient;
	private final LinkedList<String> queries;

	/**
	 * The constructor for the BulkApiQueryEngine
	 * 
	 * @param configFragment
	 *            the salesforceConfigFragment
	 * @param apiClient
	 *            the BulkApiClient used for communication
	 * @param queries
	 *            The queries defined in the configuration by the user
	 */
	public BulkApiQueryEngine(SalesforceConfigFragment configFragment, BulkApiClient apiClient,
			LinkedList<String> queries) {
		this.configFragment = configFragment;
		this.apiClient = apiClient;
		this.queries = queries;
	}

	/**
	 * The constructor for the BulkApiQueryEngine
	 * 
	 * @param configFragment
	 *            the salesforceConfigFragment
	 * @param apiClient
	 *            the BulkApiClient used for communication
	 */
	public BulkApiQueryEngine(SalesforceConfigFragment configFragment, BulkApiClient apiClient) {
		this(configFragment, apiClient, new LinkedList<>(List.of(configFragment.getBulkApiQueries().split(";"))));
	}

	/**
	 * GetRecords takes the preconfigured queries and executes those queries in
	 * order until no records are left to be consumed.
	 * 
	 * @return a Stream of records
	 */
	public Iterator<BulkApiSourceData> getRecords() {

		for (String query : queries) {
			// Submit the job
			String jobId = apiClient.submitQueryJob(query);
			var queryResult = apiClient.queryJobStatus(jobId);
			JobState state = queryResult.getState();
			// wait until the job is finished processing
			waitUntilProcessingComplete(state, jobId);
			switch (state) {
				case UploadComplete :
					return apiClient.getResultStream(jobId, null, queryResult.getObject(), queryResult.getCreatedDate())
							.iterator();
				case Aborted :
				case Failed :
				default :
					apiClient.deleteJob(jobId);
					return Collections.emptyIterator();
			}
		}
		return Collections.emptyIterator();
	}

	private void waitUntilProcessingComplete(JobState state, String jobId) {
		while (state.equals(JobState.InProgress) || state.equals(JobState.Submitted)
				|| state.equals(JobState.JobComplete)) {
			try {
				// TODO Add a max wait time before returning and updating the state to failed
				// e.g. wait for a max of 5 minutes for the job to process and then return fail
				// if it isn't returned by then
				Thread.sleep(1000);
				var queryResult = apiClient.queryJobStatus(jobId);
				state = queryResult.getState();
			} catch (InterruptedException e) {
				LOGGER.error("Attempted to sleep until job was complete but an exception was thrown: ", e);
				throw new RuntimeException(e);
			}
		}
	}

}
