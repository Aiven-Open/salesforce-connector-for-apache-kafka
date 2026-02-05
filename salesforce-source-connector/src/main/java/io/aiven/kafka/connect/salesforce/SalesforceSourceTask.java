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

import io.aiven.commons.kafka.connector.source.AbstractSourceRecordIterator;
import io.aiven.commons.kafka.connector.source.AbstractSourceTask;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.kafka.connector.source.transformer.Transformer;
import io.aiven.commons.timing.BackoffConfig;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceData;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceRecord;
import io.aiven.kafka.connect.salesforce.transformers.JsonTransformer;
import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;

import io.aiven.kafka.connect.salesforce.utils.Version;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceSourceTask.class);

	/**
	 * transformer from csv to AVRO
	 */
	private Transformer transformer; // NOPMD
	/**
	 * Iterator BulkApiSourceRecord to process individual results and returns
	 * SourceRecords defaults to empty
	 */
	private Iterator<BulkApiSourceRecord> inner = Collections.emptyIterator();

	/**
	 * The source Data iterator pulls individual records out of the Bulk Api
	 * defaults to empty
	 */
	private Iterator<BulkApiSourceData> bulkApiSourceRecordIterator = Collections.emptyIterator();
	/** The offset manager this task uses */
	private OffsetManager<SalesforceOffsetManagerEntry> offsetManager;

	/**
	 * JsonConverter
	 */
	private JsonConverter jsonConverter;

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

	/**
	 * This allows for testing to inject a context
	 * 
	 * @param context
	 *            A SourceTaskContext
	 */
	public SalesforceSourceTask(SourceTaskContext context) {
		this.context = context;
	}

	@Override
	protected SourceCommonConfig configure(final Map<String, String> props) {
		LOGGER.info("Salesforce Source task started.");
		this.salesforceSourceConfig = new SalesforceSourceConfig(props);
		offsetManager = new OffsetManager<>(context);
		/**
		 * The bulk api client for querying the Bulk api
		 */
		BulkApiClient apiClient = new BulkApiClient(salesforceSourceConfig.getSalesforceConfigFragment());
		/**
		 * The Bulk Api Query Engine handles the lifecycle of bulk api requests
		 */
		BulkApiQueryEngine engine = new BulkApiQueryEngine(salesforceSourceConfig.getSalesforceConfigFragment(),
				apiClient);
		// This should maybe be in start
		setBulkApiSourceRecordIterator(engine.getSalesforceBulkIterator());
		jsonConverter = new JsonConverter();
		jsonConverter.configure(Map.of("schemas.enable", "false"), false);
		return salesforceSourceConfig;
	}

	@Override
	protected Iterator<SourceRecord> getIterator(BackoffConfig config) {
		LOGGER.info("getIterator() query BulkApi");
		Iterator<SourceRecord> sourceRecordIterator = new Iterator<>() {

			@Override
			public boolean hasNext() {
				if (stillPolling()) {
					if (inner.hasNext()) {
						return true;
					} else if (bulkApiSourceRecordIterator.hasNext()) {
						inner = new AbstractSourceRecordIterator<>(salesforceSourceConfig,
								new JsonTransformer(jsonConverter), offsetManager, bulkApiSourceRecordIterator.next());
						return inner.hasNext();
					} else {
						return false;
					}
				}

				return false;
			}

			@Override
			public SourceRecord next() {
				final BulkApiSourceRecord bulkApiSourceRecord = inner.next();
				return bulkApiSourceRecord.getSourceRecord(salesforceSourceConfig.getErrorsTolerance(), offsetManager);
			}

		};

		return IteratorUtils.filteredIterator(sourceRecordIterator, Objects::nonNull);
	}

	private void setBulkApiSourceRecordIterator(final Iterator<BulkApiSourceData> iterator) {
		this.bulkApiSourceRecordIterator = iterator;
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
