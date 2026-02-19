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
package io.aiven.kafka.connect.salesforce.model;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This BulkApiKey is used to contain all the important information required to
 * be able to rebuild the same query again
 */
public class BulkApiKey implements Comparable<BulkApiKey> {

	private String lastExecutionTime;
	private String queryHash;
	private String apiName;

	/**
	 * Constructor
	 * 
	 * @param apiName
	 *            The name of the api
	 * @param query
	 *            The query that was used to return the data
	 * @param lastExecutionTime
	 *            The execution time this query was executed at
	 */
	public BulkApiKey(String apiName, String query, String lastExecutionTime) {
		this.apiName = apiName;
		this.queryHash = Arrays
				.toString(MurmurHash3.hash128(query.replaceAll("\\s+", "").getBytes(StandardCharsets.UTF_8)));
		this.lastExecutionTime = lastExecutionTime;
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
	 * Set the time this query was submitted to the api at
	 * 
	 * @param lastExecutionTime
	 *            The time this query was submitted against the API
	 */
	public void setLastExecutionTime(String lastExecutionTime) {
		this.lastExecutionTime = lastExecutionTime;
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
	 * Set the query which identifies what has been submitted against the salesforce
	 * api, it will be stripped of spaces and a murmur3 hash will be generated from
	 * it
	 * 
	 * @param queryHash
	 *            A String of the original query submitted against Salesforce's api
	 */
	public void setQueryHash(String queryHash) {
		this.queryHash = Arrays
				.toString(MurmurHash3.hash128(queryHash.replaceAll("\\s+", "").getBytes(StandardCharsets.UTF_8)));
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
	 * Set the api name for this key
	 * 
	 * @param apiName
	 *            the apiName
	 */
	public void setApiName(String apiName) {
		this.apiName = apiName;
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
		if (other.getApiName().compareTo(apiName) == 0) {
			if (other.getQueryHash().compareTo(queryHash) == 0) {
				return other.getLastExecutionTime().compareTo(lastExecutionTime);
			}
			return other.getQueryHash().compareTo(queryHash);
		}
		return other.getApiName().compareTo(apiName);

	}

	@Override
	public String toString() {
		return String.join(":", apiName, queryHash, lastExecutionTime);
	}
}
