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
package io.aiven.kafka.connect.salesforce.common.bulk.model;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * This BulkApiKey is used to contain all the important information required to
 * be able to rebuild the same query again
 */
public final class BulkApiKey implements Comparable<BulkApiKey> {
	private static final String SEGMENT_SEPARATOR = "/"; // can not be ':' as that is in the execution time string.
	private final String lastExecutionTime;
	private final String queryHash;
	private final String apiName;
	private final String locator;

	/**
	 * Constructor
	 *
	 * @param apiName
	 *            The name of the api
	 * @param query
	 *            The query that was used to return the data, this query will be
	 *            automatically hashed
	 * @param lastExecutionTime
	 *            The execution time this query was executed at
	 * @param locator
	 *            The locator specifies the page of results that is being returned
	 */
	public BulkApiKey(String apiName, String query, String lastExecutionTime, String locator) {
		this(apiName, query, lastExecutionTime, locator, true);
	}

	/**
	 * Constructor
	 *
	 * @param apiName
	 *            The name of the api
	 * @param query
	 *            The query that was used to return the data
	 * @param lastExecutionTime
	 *            The execution time this query was executed at
	 * @param locator
	 *            The locator specifies the page of results that is being returned
	 * @param hashQueryString
	 *            boolean to determine wether the query should be hashed can be
	 *            false if already hashed
	 */
	public BulkApiKey(String apiName, String query, String lastExecutionTime, String locator, boolean hashQueryString) {
		this.apiName = apiName;
		this.queryHash = hashQueryString
				? Arrays.toString(MurmurHash3.hash128(query.replaceAll("\\s+", "").getBytes(StandardCharsets.UTF_8)))
				: query;
		this.lastExecutionTime = lastExecutionTime;
		this.locator = locator;
	}

	/**
	 * Parses the BuildApiKey string into a BuildApiKey implementation.
	 * 
	 * @param buildApiString
	 *            the string to parse.
	 * @return parsed BuildApiKey.
	 */
	public static BulkApiKey parse(String buildApiString) {
		String[] parts = buildApiString.split("\\Q" + SEGMENT_SEPARATOR + "\\E");
		return new BulkApiKey(parts[0], parts[1], parts[2], parts[4]);
	}

	/**
	 * Get the last time this query was submitted against the api at
	 * 
	 * @return The last time this query was submitted against the api at
	 */
	public String getLastExecutionTime() {
		return lastExecutionTime;
	}

	/**
	 * Get the murmur3 hash of the original query submitted against Salesforce's api
	 * 
	 * @return A murmur3 hash of the original query submitted against Salesforce's
	 *         api
	 */
	public String getQueryHash() {
		return queryHash;
	}

	/**
	 * Get the apiName for this key
	 * 
	 * @return The apiName
	 */
	public String getApiName() {
		return apiName;
	}

	/**
	 * Get the locator object
	 * 
	 * @return The locator which specifies a page of results from the api
	 */
	public String getLocator() {
		return locator;
	}

	/**
	 * Compare two BulkApiKey's to see if they are the same
	 * 
	 * @param other
	 *            the BulkApiKey to be compared.
	 * @return an integer between 1 and -1 with 0 being equal
	 */
	@Override
	public int compareTo(BulkApiKey other) {
		int result = other.getApiName().compareTo(apiName);
		if (result == 0) {
			result = other.getQueryHash().compareTo(queryHash);
			if (result == 0) {
				result = other.getLastExecutionTime().compareTo(lastExecutionTime);
				if (result == 0) {
					result = other.getLocator().compareTo(locator);
				}
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BulkApiKey that = (BulkApiKey) o;
		return Objects.equals(lastExecutionTime, that.lastExecutionTime) && Objects.equals(queryHash, that.queryHash)
				&& Objects.equals(apiName, that.apiName) && Objects.equals(locator, that.locator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lastExecutionTime, queryHash, apiName, locator);
	}

	@Override
	public String toString() {
		return String.join(SEGMENT_SEPARATOR, apiName, queryHash, lastExecutionTime, locator);
	}
}
