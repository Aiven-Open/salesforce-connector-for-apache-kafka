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
package io.aiven.kafka.connect.salesforce.model;

import com.google.common.collect.Streams;
import io.aiven.commons.kafka.connector.source.NativeSourceData;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.task.Context;

import io.aiven.commons.timing.AbortTrigger;
import io.aiven.commons.timing.Backoff;
import io.aiven.commons.timing.BackoffConfig;
import io.aiven.commons.timing.SupplierOfLong;
import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.BulkApiQueryEngine;
import io.aiven.kafka.connect.salesforce.common.bulk.model.SalesforceContext;
import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;
import org.apache.commons.codec.digest.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This BulkApiSourceData facilitates sending BulkApi data into a SourceRecord
 * along with creating the OffsetManager entry for it.
 */
public class BulkApiSourceData extends NativeSourceData<BulkApiKey> {

	// private static final String BULK_API = "bulkApi";
	private static final Logger LOGGER = LoggerFactory.getLogger(BulkApiSourceData.class); // NOPMD
	final Backoff backoff;
	private final Duration minimumDelayBetweenQueries;
	/**
	 * This is a map of the latest lastModifiedDate for each query that has been
	 * identified We use this to get the lastModifiedDate for the next query
	 */
	private final Map<String, ZonedDateTime> lastSeenModifiedDate;
	/**
	 * Track the last time a query was executed and allows a back off to be used as
	 * to how often it can be called.
	 */
	private final Map<String, ZonedDateTime> lastQueryExecuted;
	/**
	 * The Bulk Api Query Engine handles the lifecycle of bulk api requests
	 */
	private final BulkApiQueryEngine engine;
	/**
	 * A queue of queries to execute. This is being used as a circular buffer.
	 */
	private final LinkedList<SOQLQuery> queries;
	// https://developer.salesforce.com/docs/atlas.en-us.260.0.object_reference.meta/object_reference/sforce_api_objects_concepts.htm
	// We use the lastModifiedDate to only get deltas of changes in the Bulk API

	private final static KeySerde<BulkApiKey> BULKAPIKEY_SERDE = new KeySerde<>() {
		public String toString(BulkApiKey nativeKey) {
			return nativeKey.toString();
		}

		public BulkApiKey fromString(String nativeKeyString) {
			return BulkApiKey.parse(nativeKeyString);
		}
	};
	/**
	 * Bulk Api Source Record
	 *
	 * @param config
	 *            The SalesforceSourceConfigFragment with all the relevant config
	 *            for configuring the BulkApiSourceData
	 * @param offsetManager
	 *            the offsetManager used in this implementation of BulkApiSourceData
	 * @param lastSeenModifiedDate
	 *            Map of last Seen LastModifiedDates processed
	 */
	public BulkApiSourceData(final SalesforceSourceConfig config, final OffsetManager offsetManager,
			Map<String, ZonedDateTime> lastSeenModifiedDate) {
		super(config, offsetManager);
		this.queries = config.getBulkApiQueries().stream().map(SOQLQuery::fromQueryString)
				.collect(Collectors.toCollection(LinkedList::new));
		this.lastQueryExecuted = new HashMap<>();
		this.engine = new BulkApiQueryEngine(config, new BulkApiClient(config));
		this.lastSeenModifiedDate = lastSeenModifiedDate;
		this.backoff = setupBackOffTimer();
		this.minimumDelayBetweenQueries = config.getMinimumQueryExecutionDelay();
	}

	/**
	 * Protected constructor for testing
	 * 
	 * @param config
	 *            The SalesforceSourceConfig
	 * @param offsetManager
	 *            The OffsetManager
	 * @param engine
	 *            A BulkApiQueryEngine
	 * @param lastSeenModifiedDate
	 *            last Seen LastModifiedDate processed
	 */
	protected BulkApiSourceData(final SalesforceSourceConfig config, final OffsetManager offsetManager,
			BulkApiQueryEngine engine, Map<String, ZonedDateTime> lastSeenModifiedDate) {
		super(config, offsetManager);
		this.queries = config.getBulkApiQueries().stream().map(SOQLQuery::fromQueryString)
				.collect(Collectors.toCollection(LinkedList::new));
		this.lastQueryExecuted = new HashMap<>();
		this.engine = engine;
		this.lastSeenModifiedDate = lastSeenModifiedDate;
		this.backoff = setupBackOffTimer();
		this.minimumDelayBetweenQueries = config.getMinimumQueryExecutionDelay();
	}

	private Backoff setupBackOffTimer() {
		Duration delay = Duration.ofSeconds(3);
		BackoffConfig backOffConfig = new BackoffConfig() {
			/**
			 *
			 * @return
			 */
			@Override
			public SupplierOfLong getSupplierOfTimeRemaining() {
				return delay::toMillis;
			}

			/**
			 *
			 * @return
			 */
			@Override
			public AbortTrigger getAbortTrigger() {
				return null;
			}

			/**
			 *
			 * @return
			 */
			@Override
			public boolean applyTimerRule() {
				return false;
			}
		};
		Backoff backoff = new Backoff(backOffConfig);
		backoff.setMinimumDelay(delay);
		return backoff;
	}
	/**
	 * get the source name from the data
	 * 
	 * @return the source name
	 */
	@Override
	public String getSourceName() {
		return "Salesforce Bulk API";
	}

	/**
	 * Get the native Item in a stream
	 *
	 * @param offset
	 *            the native key to start from. May be {@code null} ot indicate *
	 *            start at the beginning.
	 * @return A stream of native objects. May be empty but not {@code null}.
	 */
	@Override
	public Stream<BulkApiNativeInfo> getNativeItemStream(final BulkApiKey offset) {
		return getSalesforceBulkApiStream();
	}

	/**
	 * Creates an offset manager entry using the data in the map.
	 *
	 * @param data
	 *            the data to create the offset manager from.
	 * @return a valid offset manager entry.
	 */
	@Override
	public OffsetManager.OffsetManagerEntry createOffsetManagerEntry(final Map<String, Object> data) {
		return null;
	}

	private String getQueryHash() {

		return Arrays.toString(MurmurHash3
				.hash128(queries.getLast().getSOQLQuery().replaceAll("\\s+", "").getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Creates an offset manager entry from a context.
	 *
	 * @param context
	 *            the context to create the offset manager from.
	 * @return a valid offset manager.
	 */
	@Override
	protected OffsetManager.OffsetManagerEntry createOffsetManagerEntry(final Context context) {
		SalesforceContext ctx = (SalesforceContext) context;

		return new SalesforceOffsetManagerEntry((BulkApiKey) context.getNativeKey(), ctx.getJobId(),
				ctx.getTotalRecords(), ctx.getLastModifiedTimestamp());
	}

	/**
	 * Creates an offset manager key for the native key.
	 *
	 * @param nativeKey
	 *            THe native key to create an offset manager key for.
	 * @return An offset manager key.
	 */
	@Override
	protected OffsetManager.OffsetManagerKey getOffsetManagerKey(final BulkApiKey nativeKey) {
		return new SalesforceOffsetManagerEntry(nativeKey).getManagerKey();
	}

	/**
	 * This method allows us to back off for a while and not constantly be trying to
	 * re run the same query too quickly.
	 */
	private void backOff() {
		ZonedDateTime lastExecutedDateTime = lastQueryExecuted.getOrDefault(getQueryHash(), null);
		if (lastExecutedDateTime == null) {
			return;
		}
		while (lastExecutedDateTime.plusSeconds(minimumDelayBetweenQueries.getSeconds())
				.isAfter(ZonedDateTime.now(ZoneId.of("UTC")))) {
			backoff.cleanDelay();
		}
	}

	/**
	 * Get the KeySerde for the String
	 *
	 * @return The native Key.
	 */
	@Override
	protected Optional<KeySerde<BulkApiKey>> getNativeKeySerde() {
		return Optional.of(BULKAPIKEY_SERDE);
	}

	/**
	 * getSalesforceBulkIterator takes the preconfigured queries and executes those
	 * queries in order until no records are left to be consumed. If the iterator of
	 * results is empty on hasNext it checks if there is another query to execute
	 * and on next() it executes said query
	 *
	 * @return a stream of records
	 */
	public Stream<BulkApiNativeInfo> getSalesforceBulkApiStream() {

		return Streams.stream(new Iterator<>() {
			AtomicBoolean latch = new AtomicBoolean(false);
			/**
			 * Returns {@code true} if the iteration has more elements. (In other words,
			 * returns {@code true} if {@link #next} would return an element rather than
			 * throwing an exception.)
			 *
			 * @return {@code true} if the iteration has more elements
			 */
			@Override
			public boolean hasNext() {
				return !queries.isEmpty() && !latch.get();
			}

			/**
			 * Returns the next element in the iteration.
			 *
			 * @return the next element in the iteration
			 *
			 */
			@Override
			public BulkApiNativeInfo next() {
				// TODO this can be cleaned up a bit by changing the queries queue to the last
				// BulkApiNativeInfo and we add the last execution time to the native info then
				// we can store the execution as a long and use in the in getRecords()
				// calculation without parsing and allow the BulkApiNativeInfo to format it for
				// other purposes.
				SOQLQuery element = queries.pop();
				// Re queue to end of the list;
				queries.offerLast(element);
				while (latch.compareAndSet(false, true)) {
					LOGGER.info("delay while waiting for latch");
					backoff.cleanDelay();
				}
				// Regular back off
				backOff();
				ZonedDateTime lastModifiedDate = lastSeenModifiedDate.getOrDefault(getQueryHash(), null);

				try {
					return engine.getRecords(element, lastModifiedDate != null ? lastModifiedDate.toString() : null)
							.next();
				} finally {
					lastQueryExecuted.put(getQueryHash(), ZonedDateTime.now(ZoneId.of("UTC")));
				}
			}
		});

	}
}
