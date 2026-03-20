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
import io.aiven.commons.timing.Backoff;
import io.aiven.commons.timing.Timer;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResultResponse;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiNativeInfo;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The BulkApiQueryEngine handles taking the config from the connector and
 * making the relevant queries to the Salesforce BulkApi 2.0 It handles the
 * lifecycle of the requests along ith exceptions
 */
public class BulkApiQueryEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(BulkApiQueryEngine.class);
	private static final String BULK_API = "bulkApi";
	/**
	 * To be configured through config, the amount of time to wait in between
	 * executing the same query again.
	 */
	private final Duration statusCheckDelay;
	private final SalesforceSourceConfig config; // NOPMD i will need this
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
		this.statusCheckDelay = config.getStatusCheckWaitTime();

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
	public Iterator<BulkApiNativeInfo> getRecords(SOQLQuery query, String lastModifiedDate) {

		LOGGER.debug("Query String to execute {}", query.getQueryString(lastModifiedDate));

		// Submit the job
		Optional<String> optJobId = apiClient.submitQueryJob(query.getQueryString(lastModifiedDate));
		if (optJobId.isPresent()) {
			String jobId = optJobId.get();
			Optional<QueryResponse> optQueryResponse = apiClient.queryJobStatus(jobId);
			if (optQueryResponse.isPresent()) {
				QueryResponse queryResponse = optQueryResponse.get();
				LOGGER.debug("JobId {} , State {}, Date Created {}, error message {}", queryResponse.getId(),
						queryResponse.getState(), queryResponse.getCreatedDate(), queryResponse.getErrorMessage());
				// wait until the job is finished processing
				JobState completedState = waitUntilProcessingComplete(queryResponse.getState(), jobId);
				switch (completedState) {
					case JobComplete :
						BulkApiKey bulkApiKey = new BulkApiKey(BULK_API, query.getSOQLQuery(),
								queryResponse.getCreatedDate());
						return new FutureIterator(jobId, queryResponse.getObject(), bulkApiKey, lastModifiedDate);
					case Aborted :
					case Failed :
					default :
						LOGGER.warn("State {} returned while waiting for query which was unexpected", completedState);
						apiClient.deleteJob(jobId);
						break;
				}
			}
		}
		return Collections.emptyIterator();
	}

	private JobState waitUntilProcessingComplete(JobState state, String jobId) {
		if (state.isExecuting()) {
			Timer timer = new Timer(statusCheckDelay);
			Backoff backoff = new Backoff(timer.getBackoffConfig());
			backoff.setMinimumDelay(statusCheckDelay);

			while (state.isExecuting() && !timer.isExpired()) {
				backoff.cleanDelay();
				Optional<QueryResponse> opt = apiClient.queryJobStatus(jobId);
				if (opt.isPresent()) {
					state = opt.get().getState();
				}
			}
		}

		return state;
	}

	class FutureIterator implements Iterator<BulkApiNativeInfo> {
		private CompletableFuture<BulkApiResultResponse> bulkApiResultResponseFuture;
		private final String jobId;
		private final String object;
		private final BulkApiKey bulkApiKey;
		private final String lastModifiedDate;

		FutureIterator(final String jobId, final String object, final BulkApiKey bulkApiKey, String lastModifiedDate) {
			this.jobId = jobId;
			this.object = object;
			this.bulkApiKey = bulkApiKey;
			this.lastModifiedDate = lastModifiedDate;
			this.bulkApiResultResponseFuture = apiClient.getJobResults(jobId, null, object, bulkApiKey);
		}

		@Override
		public boolean hasNext() {
			if (bulkApiResultResponseFuture != null && !bulkApiResultResponseFuture.isDone()) {
				try {
					bulkApiResultResponseFuture.join();
				} catch (CancellationException | CompletionException e) {
					LOGGER.info("Iterator error {}", e.getMessage(), e);
					return false;
				}
			}
			return bulkApiResultResponseFuture != null && !bulkApiResultResponseFuture.isCancelled()
					&& !bulkApiResultResponseFuture.isCompletedExceptionally();
		}

		@Override
		public BulkApiNativeInfo next() {
			if (hasNext()) {
				// mo exception thrown here because hasNext() resolves the future.
				BulkApiResultResponse bulkApiResultResponse = bulkApiResultResponseFuture.join();
				final NativeInfo<BulkApiKey, String> nativeInfo = bulkApiResultResponse.getResult().getNativeInfo();
				String topic = String.format("%s.%s.%s", config.getTopicPrefix(), nativeInfo.nativeKey().getApiName(),
						bulkApiResultResponse.getResult().getObjectName());

				BulkApiNativeInfo bulkApiNativeInfo = new BulkApiNativeInfo(nativeInfo, topic, null, null, jobId,
						bulkApiResultResponse.getNumberOfRecords(), lastModifiedDate);

				if (StringUtils.isNotBlank(bulkApiResultResponse.getLocator())) {
					bulkApiResultResponseFuture = apiClient.getJobResults(jobId, bulkApiResultResponse.getLocator(),
							object, bulkApiKey);
				} else {
					bulkApiResultResponseFuture = null;
				}
				return bulkApiNativeInfo;
			}
			throw new NoSuchElementException();
		}
	}
}
