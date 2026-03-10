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

import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.BulkApiQueryEngine;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This BulkApiSourceData facilitates sending BulkApi data into a SourceRecord
 * along with creating the OffsetManager entry for it.
 */
public class BulkApiSourceData extends NativeSourceData<BulkApiKey> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BulkApiSourceData.class);
	/**
	 * The Bulk Api Query Engine handles the lifecycle of bulk api requests
	 */
	private final BulkApiQueryEngine engine;
	/**
	 * A queue of queries to execute. This is being used as a circular buffer.
	 */
	private final LinkedList<String> queries;
	// https://developer.salesforce.com/docs/atlas.en-us.260.0.object_reference.meta/object_reference/sforce_api_objects_concepts.htm
	// We use the lastModifiedDate to only get deltas of changes in the Bulk API
	private final Map<String, String> lastExecutionTime;

	/**
	 * Bulk Api Source Record
	 *
	 * @param config
	 *            The SalesforceSourceConfigFragment with all the relevant config
	 *            for configuring the BulkApiSourceData
	 * @param offsetManager
	 *            the offsetManager used in this implementation of BulkApiSourceData
	 */
	public BulkApiSourceData(final SalesforceSourceConfig config, final OffsetManager offsetManager) {
		super(config, offsetManager);
		this.queries = new LinkedList<>(config.getBulkApiQueries());

		this.engine = new BulkApiQueryEngine(config, new BulkApiClient(config));
		this.lastExecutionTime = new HashMap<>();
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
		return new SalesforceOffsetManagerEntry(new BulkApiKey("bulkapi", queries.getLast(), ""), data);
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
		return new SalesforceOffsetManagerEntry((BulkApiKey) context.getNativeKey());
	}

	/**
	 * extracts the native Key from the string representation.
	 *
	 * @param keyString
	 *            the keyString.
	 * @return The native Key.
	 */
	@Override
	protected BulkApiKey parseNativeKey(final String keyString) {
		return BulkApiKey.parse(keyString);
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
	 * getSalesforceBulkIterator takes the preconfigured queries and executes those
	 * queries in order until no records are left to be consumed. If the iterator of
	 * results is empty on hasNext it checks if there is another query to execute
	 * and on next() it executes said query
	 *
	 * @return a stream of records
	 */
	public Stream<BulkApiNativeInfo> getSalesforceBulkApiStream() {

		return Streams.stream(new Iterator<>() {

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
			public BulkApiNativeInfo next() {
				// TODO this can be cleaned up a bit by changing the queries queue to the last
				// BulkApiNativeInfo and we add the last execution time to the native info then
				// we can store the execution as a long and use in the in getRecords()
				// calculation without parsing and allow the BulkApiNativeInfo to format it for
				// other purposes.
				String element = queries.pop();
				// Re queue to end of the list
				LOGGER.debug("Get Records for Query {}", element);
				queries.offerLast(element);
				String newLastModifiedDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
				try {
					return engine.getRecords(element, lastExecutionTime.getOrDefault(element, null)).next();
				} finally {
					lastExecutionTime.put(element, newLastModifiedDate);
				}
			}
		});

	}
}
