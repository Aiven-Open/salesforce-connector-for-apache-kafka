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
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the Salesforce Offset manager Entry class, it handles creating the
 * offsets which are stored in Kafka and allows us to read themback out again as
 * well so we can make sure we don't re-read the data as well.
 */
public class SalesforceOffsetManagerEntry implements OffsetManager.OffsetManagerEntry {

	/**
	 * Specifies if the data is coming from the bulk api, streaming api or pub/sub
	 * api
	 */
	public static final String API_NAME = "apiName";
	/**
	 * ObjectName refer to the name of the table Objects in Salesforce e.g. Account,
	 * Address, etc
	 */
	public static final String QUERY_HASH = "queryHash";
	/**
	 * The time this query was executed against salesforce allowing us to know when
	 * the data was queried
	 */
	public static final String QUERY_EXECUTION_TIME = "queryExecutionTime";
	/**
	 * The number of records read in this interaction
	 */
	public static final String ID = "id";
	/**
	 * Defines whether a query has completed processing or not
	 */
	public static final String IS_COMPLETE = "isComplete";
	static final List<String> RESTRICTED_KEYS = List.of(ID, IS_COMPLETE);
	/** The data map that stores all the values */
	private final Map<String, Object> data = new HashMap<>();
	private final BulkApiKey bulkApiKey;
	private String id;
	private int recordCount;

	/**
	 * Construct the SalesforceOffsetManagerEntry.
	 *
	 * @param bulkApiKey
	 *            the Object the entry comes from
	 */
	public SalesforceOffsetManagerEntry(final BulkApiKey bulkApiKey) {
		this.bulkApiKey = bulkApiKey;
	}

	/**
	 * Constructs an OffsetManagerEntry from an existing map. Used to reconstitute
	 * previously serialized SalesforceOffsetManagerEntry. used by
	 * {@link #fromProperties(Map)}
	 * 
	 * @param bulkApiKey
	 *            the Object the entry comes from
	 * @param properties
	 *            the property map.
	 */
	public SalesforceOffsetManagerEntry(final BulkApiKey bulkApiKey, final Map<String, Object> properties) {
		this(bulkApiKey);
		data.putAll(properties);
		data.putIfAbsent(IS_COMPLETE, false);

	}

	/**
	 *
	 * Get a key for the OffsetManagerKey
	 *
	 * @param bulkApiKey
	 *            the Object the entry comes from
	 * @return a new instance of OffsetManagerKey
	 */
	public static OffsetManager.OffsetManagerKey asKey(final BulkApiKey bulkApiKey) {
		return () -> Map.of(API_NAME, bulkApiKey.getApiName(), QUERY_HASH, bulkApiKey.getQueryHash());
	}
	/**
	 * Creates an SalesforceOffsetManagerEntry. Will return {@code null} if
	 * properties is {@code null}.
	 *
	 * @param properties
	 *            the properties to wrap. May be {@code null}.
	 * @return an SalesforceOffsetManagerEntry.
	 * @throws IllegalArgumentException
	 *             if one of the {@link #RESTRICTED_KEYS} is missing.
	 */
	@Override
	public SalesforceOffsetManagerEntry fromProperties(final Map<String, Object> properties) {
		if (properties == null) {
			return null;
		}
		return new SalesforceOffsetManagerEntry(bulkApiKey, properties);
	}

	/**
	 * Creates a new offset map. No defensive copy is necessary.
	 *
	 * @return a new map of properties and values.
	 */
	@Override
	public Map<String, Object> getProperties() {
		final Map<String, Object> result = new HashMap<>(data);
		result.put(ID, id);
		result.put(QUERY_EXECUTION_TIME, bulkApiKey.getLastExecutionTime());
		// TODO to be updated when csv file is processed to true.
		result.put(IS_COMPLETE, false);
		return result;
	}

	@Override
	public Object getProperty(final String key) {
		if (ID.equals(key)) {
			return id;
		}
		return data.get(key);
	}

	@Override
	public void setProperty(final String property, final Object value) {
		if (RESTRICTED_KEYS.contains(property)) {
			throw new IllegalArgumentException(
					String.format("'%s' is a restricted key and may not be set using setProperty()", property));
		}
		data.put(property, value);
	}

	/**
	 * Returns the OffsetManagerKey for this Entry.
	 *
	 * @return the OffsetManagerKey for this Entry.
	 */
	@Override
	public OffsetManager.OffsetManagerKey getManagerKey() {
		return asKey(bulkApiKey);
	}

	@Override
	public void incrementRecordCount() {
		recordCount++;
	}

	/**
	 * Gets the number of records extracted from data returned from Salesforce.
	 *
	 * @return the number of records extracted from data returned from Salesforce.
	 */
	@Override
	public long getRecordCount() {
		return recordCount;
	}

	/**
	 * Gets the Salesforce api used for the current object.
	 *
	 * @return the Salesforce api used for the current object.
	 */
	public String getApiName() {
		return bulkApiKey.getApiName();
	}

	/**
	 * Gets the name for the current object.
	 *
	 * @return the name for the current object.
	 */
	public BulkApiKey getBulkApiKey() {
		return bulkApiKey;
	}

	/**
	 * Gets the time the for the current object that it was queried at.
	 *
	 * @return the time the for the current object that it was queried at.
	 */
	public BulkApiKey getLastExecutionTime() {
		return bulkApiKey;
	}

	/**
	 * CompareTo method implementation for SalesforceOffsetManagerEntry
	 * 
	 * @param other
	 *            SalesforceOffsetManagerEntry that this is being compared against
	 *            this instance
	 * @return 0 if match non 0 if it does not match
	 */
	public int compareTo(SalesforceOffsetManagerEntry other) {

		if (this == other) { // NOPMD comparing instance
			return 0;
		}
		int result = getApiName().compareTo(other.getApiName());
		if (result == 0) {
			result = getBulkApiKey().compareTo(other.getBulkApiKey());
			if (result == 0) {
				result = getLastExecutionTime().compareTo(other.getLastExecutionTime());
				if (result == 0) {
					result = Long.compare(getRecordCount(), other.getRecordCount());
				}
			}
		}
		return result;
	}

}
