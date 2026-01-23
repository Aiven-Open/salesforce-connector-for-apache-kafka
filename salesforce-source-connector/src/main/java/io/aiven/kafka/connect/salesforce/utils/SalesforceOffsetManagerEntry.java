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
package io.aiven.kafka.connect.salesforce.utils;

import io.aiven.commons.kafka.connector.source.OffsetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is the Salesforce Offset manager Entry class, it handles creating the
 * offsets which are stored in Kafka and allows us to read themback out again as
 * well so we can make sure we don't re-read the data as well.
 */
public class SalesforceOffsetManagerEntry implements OffsetManager.OffsetManagerEntry<SalesforceOffsetManagerEntry> {

	/**
	 * Logger for the SalesforceOffsetManagerEntry
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceOffsetManagerEntry.class);
	/**
	 * Specifies if the data is coming from the bulk api, streaming api or pub/sub
	 * api
	 */
	public static final String API_CLIENT = "apiClient";
	/**
	 * ObjectName refer to the name of the table Objects in Salesforce e.g. Account,
	 * Address, etc
	 */
	public static final String OBJECT_NAME = "objectName";
	/**
	 * Last time we received information on this record
	 */
	public static final String LAST_ACTIVITY = "lastActivity";
	/**
	 * The number of records read in this interaction
	 */
	public static final String RECORD_COUNT = "recordCount";

	/**
	 * Default constructor
	 */
	public SalesforceOffsetManagerEntry() {
		LOGGER.info("Create a new Salesforce Offset manager Entry");
	}

	@Override
	public SalesforceOffsetManagerEntry fromProperties(Map<String, Object> map) {
		return null;
	}

	@Override
	public Map<String, Object> getProperties() {
		return Map.of();
	}

	@Override
	public Object getProperty(String s) {
		return null;
	}

	@Override
	public void setProperty(String s, Object o) {

	}

	@Override
	public OffsetManager.OffsetManagerKey getManagerKey() {
		return null;
	}

	@Override
	public void incrementRecordCount() {

	}

	@Override
	public long getRecordCount() {
		return 0;
	}

	@Override
	public int compareTo(SalesforceOffsetManagerEntry o) {
		return 0;
	}
}
