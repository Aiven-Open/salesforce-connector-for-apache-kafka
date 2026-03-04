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
package io.aiven.kafka.connect.salesforce.sink.config;

import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.commons.kafka.connector.common.config.ConnectorCommonConfig;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfig;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;

import java.util.Map;

/**
 * The configuration for the Salesforce sink Connector
 */
public final class SalesforceSinkConfig extends ConnectorCommonConfig implements SalesforceCommonConfig {

	private final SalesforceCommonConfigFragment commonFragment;
	private final SalesforceSinkConfigFragment sinkFragment;

	/**
	 * Instantiation of the configuration.
	 *
	 * @param originals
	 *            The original configuration that is stored in a Map of String,
	 *            String
	 */
	public SalesforceSinkConfig(Map<String, String> originals) {
		super(new SalesforceSinkConfigDef(), originals);
		FragmentDataAccess dataAccess = FragmentDataAccess.from(this);
		commonFragment = new SalesforceCommonConfigFragment(dataAccess);
		sinkFragment = new SalesforceSinkConfigFragment(dataAccess);
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
	 * Gets the destination object type that will be written to Salesforce (such as
	 * Account or Contact).
	 *
	 * @return The destination object type to be written to Salesforce.
	 */
	public String getSinkObject() {
		return sinkFragment.getSinkObject();
	}
}
