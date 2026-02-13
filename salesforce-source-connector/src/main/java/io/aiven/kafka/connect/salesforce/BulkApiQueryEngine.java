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

import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The BulkApiQueryEngine handles taking the config from the connector and
 * making the relevant queries to the Salesforce BulkApi 2.0 It handles the
 * lifecycle of the requests along ith exceptions
 */
public class BulkApiQueryEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(BulkApiQueryEngine.class);
	private static final int WAIT_BETWEEN_QUERIES = 5000;
	/**
	 * To be configured through config, the amount of time to wait in between
	 * executing the same query again.
	 */
	private static final int DELTA_BETWEEN_QUERIES = 60000;
	private static final String WHERE_LAST_MODIFIED_DATE = "WHERE LastModifiedDate > ";
	private SalesforceConfigFragment configFragment; // NOPMD
	private final BulkApiClient apiClient;
	private final LinkedList<String> queries;
	// https://developer.salesforce.com/docs/atlas.en-us.260.0.object_reference.meta/object_reference/sforce_api_objects_concepts.htm
	// We use the lastModifiedDate to only get deltas of changes in the Bulk API
	private final Map<String, String> lastExecutionTime;

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
		this.lastExecutionTime = new HashMap<>();
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
	 * @param query
	 *            The query to execute against the Bulk Api
	 * @return a Stream of records
	 */
	private Iterator<BulkApiSourceData> getRecords(String query) {
		String lastModifiedDate = lastExecutionTime.getOrDefault(query, null);
		if (lastModifiedDate != null && LocalDateTime.now().plusSeconds(DELTA_BETWEEN_QUERIES)
				.isBefore(LocalDateTime.parse(lastModifiedDate))) {
			try {
				// Look at using backoff class
				LOGGER.info("Waiting to re-execute query");
				Thread.sleep(WAIT_BETWEEN_QUERIES);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return Collections.emptyIterator();
		}
		// Submit the job
		String jobId = apiClient.submitQueryJob(updateQuery(query, lastModifiedDate));
		var queryResult = apiClient.queryJobStatus(jobId);
		JobState state = queryResult.getState();
		// wait until the job is finished processing
		JobState completedState = waitUntilProcessingComplete(state, jobId);
		switch (completedState) {
			case UploadComplete :
				LOGGER.warn("Upload complete State returned while waiting for query which was unexpected");
			case JobComplete :
				return apiClient.getResultStream(jobId, null, queryResult.getObject(), queryResult.getCreatedDate(),
						query, lastExecutionTime).iterator();
			case Aborted :
			case Failed :
			default :
				LOGGER.warn("State {} returned while waiting for query which was unexpected", completedState);
				apiClient.deleteJob(jobId);
				return Collections.emptyIterator();
		}

	}

	private static @NonNull String updateQuery(String query, String lastModifiedDate) {
		if (StringUtils.isBlank(lastModifiedDate)) {
			return query;
		}
		// TODO Need to look at doing this smarter, if they already have a WHERE clause
		// it needs to be added etc.

		if (query.contains("WHERE")) {
			// If the where already exists then we replace it add our part of the query and
			// then add an AND to append the existing query.
			return query.replace("WHERE", WHERE_LAST_MODIFIED_DATE + lastModifiedDate + "AND ");
		} else {
			return query + " " + WHERE_LAST_MODIFIED_DATE + lastModifiedDate;
		}
	}

	/**
	 * getSalesforceBulkIterator takes the preconfigured queries and executes those
	 * queries in order until no records are left to be consumed. If the iterator of
	 * results is empty on hasNext it checks if there is another query to execute
	 * and on next() it executes said query
	 * 
	 * @return an Iterator of records
	 */
	public Iterator<BulkApiSourceData> getSalesforceBulkIterator() {

		return new Iterator<BulkApiSourceData>() {

			/**
			 * Returns {@code true} if the iteration has more elements. (In other words,
			 * returns {@code true} if {@link #next} would return an element rather than
			 * throwing an exception.)
			 *
			 * @return {@code true} if the iteration has more elements
			 */
			@Override
			public boolean hasNext() {
				return !queries.isEmpty();
			}

			/**
			 * Returns the next element in the iteration.
			 *
			 * @return the next element in the iteration
			 *
			 */
			@Override
			public BulkApiSourceData next() {
				String element = queries.pop();
				// Re queue to end of the list
				LOGGER.info("Get next query {}", element);
				queries.offerLast(element);
				return getRecords(element).next();
			}
		};

	}

	private JobState waitUntilProcessingComplete(JobState state, String jobId) {
		while (state.equals(JobState.InProgress) || state.equals(JobState.Submitted)
				|| state.equals(JobState.UploadComplete)) {
			try {
				// TODO Add a max wait time before returning and updating the state to failed
				// e.g. wait for a max of 5 minutes for the job to process and then return fail
				// if it isn't returned by then
				Thread.sleep(WAIT_BETWEEN_QUERIES);
				var queryResult = apiClient.queryJobStatus(jobId);
				state = queryResult.getState();
			} catch (InterruptedException e) {
				LOGGER.error("Attempted to sleep until job was complete but an exception was thrown: ", e);
				throw new RuntimeException(e);
			}
		}
		return state;
	}

}
