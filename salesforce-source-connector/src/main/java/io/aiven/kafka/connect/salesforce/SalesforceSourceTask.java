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
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceRecord;
import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * The salesforce source task is called by the kafka connect framework to start
 * the Salesforce source connector. It configures the connector and starts the
 * task.
 */
public class SalesforceSourceTask extends AbstractSourceTask {
	private static Logger LOGGER = LoggerFactory.getLogger(SalesforceSourceTask.class);

	/**
	 * The client for all communication with the Bulk Api queries
	 */
	private BulkApiClient bulkApiClient;
	/** The offset manager this task uses */
	private OffsetManager<SalesforceOffsetManagerEntry> offsetManager;
	/**
	 * SalesforceSourceConfig which has all the configuration for the source
	 * connector
	 */
	private SalesforceSourceConfig salesforceSourceConfig;
	/** An iterator or S3SourceRecords */
	private Iterator<BulkApiSourceRecord> bulkApiSourceRecordIterator;

	/**
	 * Should check about adding this
	 */
	public SalesforceSourceTask() {
		// super(LOGGER);
	}

	@Override
	protected Iterator<SourceRecord> getIterator(io.aiven.commons.timing.BackoffConfig backoffConfig) {
		return null;
	}

	@Override
	protected SourceCommonConfig configure(Map<String, String> map) {
		return null;
	}

	@Override
	protected void closeResources() {

	}

	@Override
	public String version() {
		return "";
	}
}
