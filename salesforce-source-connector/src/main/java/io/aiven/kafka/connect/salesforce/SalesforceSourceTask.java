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

import io.aiven.commons.kafka.connector.source.AbstractSourceTask;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.timing.Backoff;
import io.aiven.commons.timing.BackoffConfig;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceRecord;
import io.aiven.kafka.connect.salesforce.utils.Version;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.IteratorUtils;

/**
 * The salesforce source task is called by the kafka connect framework to start
 * the Salesforce source connector. It configures the connector and starts the
 * task.
 */
public class SalesforceSourceTask extends AbstractSourceTask {
	private static Logger LOGGER = LoggerFactory.getLogger(SalesforceSourceTask.class);

	// /**
	// * The client for all communication with the Bulk Api queries
	// */
	// private BulkApiClient bulkApiClient;
	/**
	 * Iterator BulkApiSourceRecord to process results from the Bulk Api
	 */
	private Iterator<BulkApiSourceRecord> bulkApiSourceRecordIterator;
	// /** The offset manager this task uses */
	// private OffsetManager<SalesforceOffsetManagerEntry> offsetManager;
	/**
	 * SalesforceSourceConfig which has all the configuration for the source
	 * connector
	 */
	private SalesforceSourceConfig salesforceSourceConfig;

	/**
	 * Should check about adding this
	 */
	public SalesforceSourceTask() {

	}

	@Override
	protected SourceCommonConfig configure(final Map<String, String> props) {
		LOGGER.info("S3 Source task started.");
		this.salesforceSourceConfig = new SalesforceSourceConfig(props);

		// offsetManager = new OffsetManager<>(context);

		return salesforceSourceConfig;
	}

	@Override
	protected Iterator<SourceRecord> getIterator(BackoffConfig config) {
		LOGGER.info("getIterator() query BulkApi");
		Iterator<SourceRecord> sourceRecordIterator = new Iterator<SourceRecord>() {

			/**
			 * The backoff for exceptions
			 */
			final Backoff backoff = new Backoff(config);

			@Override
			public boolean hasNext() {
				return bulkApiSourceRecordIterator.hasNext();
			}

			@Override
			public SourceRecord next() {
				// return bulkApiSourceRecordIterator.next();
				return null;
			}

		};

		return IteratorUtils.filteredIterator(sourceRecordIterator, Objects::nonNull);
	}

	@Override
	protected void closeResources() {

	}

	@Override
	public String version() {
		return Version.VERSION;
	}

	@Override
	public void commit() {
		LOGGER.info("Committed all records through last poll()");
	}

}
