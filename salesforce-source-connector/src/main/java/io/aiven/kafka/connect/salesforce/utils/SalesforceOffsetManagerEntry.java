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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the Salesforce Offset manager Entry class, it handles creating the
 * offsets which are stored in Kafka and allows us to read themback out again as
 * well so we can make sure we don't re-read the data as well.
 */
public class SalesforceOffsetManagerEntry implements OffsetManager.OffsetManagerEntry<SalesforceOffsetManagerEntry> {

	/**
	 * Specifies if the data is coming from the bulk api, streaming api or pub/sub
	 * api
	 */
	public static final String API_NAME = "apiName";
	/**
	 * ObjectName refer to the name of the table Objects in Salesforce e.g. Account,
	 * Address, etc
	 */
	public static final String OBJECT_NAME = "objectName";
	/**
	 * The time this query was executed against salesforce allowing us to know when
	 * the data was queried
	 */
	public static final String QUERY_EXECUTION_TIME = "queryExecutionTime";
	/**
	 * The number of records read in this interaction
	 */
	public static final String RECORD_COUNT = "recordCount";
	static final List<String> RESTRICTED_KEYS = List.of(RECORD_COUNT);
	/** The data map that stores all the values */
	private final Map<String, Object> data;
	private final String apiName;
	private final String objectName;
	private final String queryExecutionTime;
	private long recordCount;

	/**
	 * Construct the SalesforceOffsetManagerEntry.
	 *
	 * @param apiName
	 *            the api we are using.
	 * @param objectName
	 *            the Object the entry comes from
	 * @param queryExecutionTime
	 *            the time the query was executed at and thus the entry comes from
	 */
	public SalesforceOffsetManagerEntry(final String apiName, final String objectName,
			final String queryExecutionTime) {
		this.apiName = apiName;
		this.objectName = objectName;
		this.queryExecutionTime = queryExecutionTime;
		data = new HashMap<>();
	}

	/**
	 * Constructs an OffsetManagerEntry from an existing map. Used to reconstitute
	 * previously serialized SalesforceOffsetManagerEntry. used by
	 * {@link #fromProperties(Map)}
	 * 
	 * @param apiName
	 *            the api we are using.
	 * @param objectName
	 *            the Object the entry comes from
	 * @param queryExecutionTime
	 *            the time the query was executed at and thus the entry comes from
	 * @param properties
	 *            the property map.
	 */
	private SalesforceOffsetManagerEntry(final String apiName, final String objectName, final String queryExecutionTime,
			final Map<String, Object> properties) {
		this(apiName, objectName, queryExecutionTime);
		data.putAll(properties);
		final Object recordCountProperty = data.computeIfAbsent(RECORD_COUNT, s -> 0L);
		if (recordCountProperty instanceof Number) {
			recordCount = ((Number) recordCountProperty).longValue();
		}
	}

	/**
	 *
	 * Get a key for the OffsetManagerKey
	 *
	 * @param apiName
	 *            the api we are using.
	 * @param objectName
	 *            the Object the entry comes from
	 * @param queryExecutionTime
	 *            the time the record was executed at
	 * @return a new instance of OffsetManagerKey
	 */
	public static OffsetManager.OffsetManagerKey asKey(final String apiName, final String objectName,
			final String queryExecutionTime) {
		return () -> Map.of(API_NAME, apiName, OBJECT_NAME, objectName);
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
		return new SalesforceOffsetManagerEntry(apiName, objectName, queryExecutionTime, properties);
	}

	/**
	 * Creates a new offset map. No defensive copy is necessary.
	 *
	 * @return a new map of properties and values.
	 */
	@Override
	public Map<String, Object> getProperties() {
		final Map<String, Object> result = new HashMap<>(data);
		result.put(RECORD_COUNT, recordCount);
		result.put(QUERY_EXECUTION_TIME, queryExecutionTime);
		return result;
	}

	@Override
	public Object getProperty(final String key) {
		if (RECORD_COUNT.equals(key)) {
			return recordCount;
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
		return () -> Map.of(API_NAME, apiName, OBJECT_NAME, objectName);
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
		return apiName;
	}

	/**
	 * Gets the name for the current object.
	 *
	 * @return the name for the current object.
	 */
	public String getObjectName() {
		return objectName;
	}

	/**
	 * Gets the time the for the current object that it was queried at.
	 *
	 * @return the time the for the current object that it was queried at.
	 */
	public String getQueryExecutionTime() {
		return objectName;
	}

	@Override
	public int compareTo(SalesforceOffsetManagerEntry other) {

		if (this == other) { // NOPMD comparing instance
			return 0;
		}
		int result = getApiName().compareTo(other.getApiName());
		if (result == 0) {
			result = getObjectName().compareTo(other.getObjectName());
			if (result == 0) {
				result = getQueryExecutionTime().compareTo(other.getQueryExecutionTime());
				if (result == 0) {
					result = Long.compare(getRecordCount(), other.getRecordCount());
				}
			}
		}
		return result;
	}

}
