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
package io.aiven.kafka.connect.salesforce.config;

import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfig;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;

import java.util.List;
import java.util.Map;

/**
 * Salesforce Common Config to instantiate all the required configuration for
 * the salesforce connector that is common to a source and a sink connector.
 */
public final class SalesforceSourceConfig extends SourceCommonConfig implements SalesforceCommonConfig {

	private final SalesforceCommonConfigFragment commonFragment;
	private final SalesforceSourceConfigFragment sourceFragment;
	/**
	 * Instantiation of the SalesforceCommonConfig class
	 * 
	 * @param originals
	 *            The original configuration that is stored in a Map of String,
	 *            String
	 */
	public SalesforceSourceConfig(Map<String, String> originals) {
		super(new SalesforceSourceConfigDef(), originals);
		FragmentDataAccess dataAccess = FragmentDataAccess.from(this);
		commonFragment = new SalesforceCommonConfigFragment(dataAccess);
		sourceFragment = new SalesforceSourceConfigFragment(dataAccess);
	}

	@Override
	public String getOauthClientId() {
		return commonFragment.getOauthClientId();
	}

	@Override
	public String getOauthClientSecret() {
		return commonFragment.getOauthClientSecret();
	}

	@Override
	public String getSalesforceUri() {
		return commonFragment.getSalesforceUri();
	}

	@Override
	public String getSalesforceApiVersion() {
		return commonFragment.getSalesforceApiVersion();
	}

	@Override
	public int getSalesforceMaxRecords() {
		return commonFragment.getSalesforceMaxRecords();
	}

	@Override
	public String getSalesforceOauthUri() {
		return commonFragment.getSalesforceOauthUri();
	}

	@Override
	public String getTopicPrefix() {
		return commonFragment.getTopicPrefix();
	}

	@Override
	public int getSalesforceMaxRetries() {
		return commonFragment.getSalesforceMaxRetries();
	}

	/**
	 * The time to wait in between querying the status of a job
	 *
	 * @return The time in seconds to wait between checking for the status of a bulk
	 *         api job.
	 */
	@Override
	public int getStatusCheckWaitTime() {
		return commonFragment.getSalesforceStatusCheckWait();
	}

	/**
	 *
	 * @return The minimum time in seconds to wait between resubmitting the same
	 *         SOQL query
	 *
	 */
	@Override
	public int getMinimumQueryExecutionDelay() {
		return commonFragment.getSalesforceWaitBetweenQueries();
	}

	/**
	 * The last ModifiedStartDateTime is used to determine what records in an object
	 * to return can be null.
	 *
	 * @return The lastModifiedStartDateTime
	 *
	 */
	public String getLastModifiedStartDateTime() {
		return sourceFragment.getSalesforceLastModifiedStartDate();
	}

	/**
	 * Gets the list of queries to pass to the bulk api.
	 * 
	 * @return a list of queries.
	 */
	public List<String> getBulkApiQueries() {
		return sourceFragment.getBulkApiQueries();
	}
}
