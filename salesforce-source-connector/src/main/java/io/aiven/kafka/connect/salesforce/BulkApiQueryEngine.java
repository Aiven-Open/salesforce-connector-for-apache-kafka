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
package io.aiven.kafka.connect.salesforce;

import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiNativeInfo;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;

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
	private SalesforceSourceConfig config; // NOPMD i will need this
	private final io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient apiClient;

	/**
	 * The constructor for the BulkApiQueryEngine
	 * 
	 * @param config
	 *            the salesforceConfig
	 * @param apiClient
	 *            the BulkApiClient used for communication
	 */
	public BulkApiQueryEngine(SalesforceSourceConfig config,
			io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient apiClient) {
		this.config = config;
		this.apiClient = apiClient;
	}

	/**
	 * GetRecords takes the preconfigured queries and executes those queries in
	 * order until no records are left to be consumed.
	 * 
	 * @param query
	 *            The query to execute against the Bulk Api
	 * @param lastModifiedDate
	 *            The last time this query was executed, null to be used to return
	 *            everything in the object, add a ZonedDateTime to query from a
	 *            particular time
	 * @return a Stream of records
	 */
	public Iterator<BulkApiNativeInfo> getRecords(String query, String lastModifiedDate) {

		LOGGER.info("lastModifiedDate {}", lastModifiedDate);
		if (lastModifiedDate != null && ZonedDateTime.now().plusSeconds(DELTA_BETWEEN_QUERIES)
				.isBefore(ZonedDateTime.parse(lastModifiedDate))) {
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
			case JobComplete :
				return apiClient
						.getResultStream(jobId, null, queryResult.getObject(), queryResult.getCreatedDate(), query)
						.map(barr -> {
							final NativeInfo<io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey, String> nativeInfo = barr
									.getResult().getNativeInfo();
							String topic = String.format("%s%s.%s", config.getTopicPrefix(),
									nativeInfo.nativeKey().getApiName(), barr.getResult().getObjectName());
							return new BulkApiNativeInfo(nativeInfo, topic, null, null);
						}).iterator();
			case Aborted :
			case Failed :
			default :
				LOGGER.warn("State {} returned while waiting for query which was unexpected", completedState);
				apiClient.deleteJob(jobId);
				return Collections.emptyIterator();
		}
	}

	private String updateQuery(String query, String lastModifiedDate) {
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
			LOGGER.info("Actual query {}", query + " " + WHERE_LAST_MODIFIED_DATE + lastModifiedDate);
			return query + " " + WHERE_LAST_MODIFIED_DATE + lastModifiedDate;
		}
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
