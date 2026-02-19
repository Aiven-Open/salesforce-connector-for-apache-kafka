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
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecordIterator;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.kafka.connector.source.config.SourceConfigFragment;
import io.aiven.commons.kafka.connector.source.transformer.CsvTransformer;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceData;

import io.aiven.kafka.connect.salesforce.utils.Version;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The salesforce source task is called by the kafka connect framework to start
 * the Salesforce source connector. It configures the connector and starts the
 * task.
 */
public final class SalesforceSourceTask extends AbstractSourceTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceSourceTask.class);

	/** The offset manager this task uses */
	private OffsetManager offsetManager;

	/**
	 * SalesforceSourceConfig which has all the configuration for the source
	 * connector
	 */
	private SalesforceSourceConfig salesforceSourceConfig;

	/**
	 * Should check about adding this
	 */
	public SalesforceSourceTask() {
		super();
	}

//	/**
//	 * This allows for testing to inject a context
//	 *
//	 * @param context
//	 *            A SourceTaskContext
//	 */
	// USE SalesforceSourceTask.initialize(SourceTaskContext context);
//	public SalesforceSourceTask(SourceTaskContext context) {
//		this.context = context;
//	}

	/**
	 * Called by {@link #start} to allows the concrete implementation to configure
	 * itself based on properties.
	 *
	 * @param props
	 *            The properties to use for configuration.
	 * @param offsetManager
	 *            the OffsetManager to use.
	 * @return A SourceCommonConfig based configuration.
	 */
	@Override
	protected SourceCommonConfig configure(Map<String, String> props, OffsetManager offsetManager) {
		LOGGER.info("Salesforce Source task started.");
		this.offsetManager = new OffsetManager(context);
		// set the csv transformer for bulk api
		SourceConfigFragment.setter(props).transformerClass(CsvTransformer.class);
		return new SalesforceSourceConfig(props);
	}

	/**
	 * Gets the iterator of SourceRecords. The iterator that SourceRecords are
	 * extracted from for a poll event. When this iterator runs out of records it
	 * should attempt to reset and read more records from the backend on the next
	 * {@code hasNext()} call. In this way it should detect when new data has been
	 * added to the backend and continue processing.
	 * <p>
	 * This method should handle any backend exception that can be retried. Any
	 * runtime exceptions that are thrown when this iterator executes may cause the
	 * task to abort.
	 * </p>
	 *
	 * @param config
	 *            the SourceCommonConfig instance.
	 * @return The iterator of SourceRecords.
	 */
	@Override
	protected EvolvingSourceRecordIterator getIterator(SourceCommonConfig config) {
		LOGGER.info("getIterator() query BulkApi");
		SalesforceSourceConfig myConfig = (SalesforceSourceConfig) config;
		return new EvolvingSourceRecordIterator(myConfig, new BulkApiSourceData(myConfig, offsetManager));
	}

	@Override
	protected void closeResources() {
		// no resources to close
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
