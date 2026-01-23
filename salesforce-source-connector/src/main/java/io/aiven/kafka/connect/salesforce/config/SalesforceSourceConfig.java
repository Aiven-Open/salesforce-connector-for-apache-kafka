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
import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import java.util.Map;

/**
 * Salesforce Common Config to instantiate all the required configuration for
 * the salesforce connector that is common to a source and a sink connector.
 */
public final class SalesforceSourceConfig extends SourceCommonConfig implements SalesforceCommonConfig {

	private final SalesforceConfigFragment configFragment;
	/**
	 * Instantiation of the SalesforceCommonConfig class
	 * 
	 * @param originals
	 *            The original configuration that is stored in a Map of String,
	 *            String
	 */
	public SalesforceSourceConfig(Map<String, String> originals) {
		super(new SourceCommonConfigDef(), originals); // NOPMD
		configFragment = new SalesforceConfigFragment(FragmentDataAccess.from(this));
	}

	@Override
	public String getOauthClientId() {
		return configFragment.getOauthClientId();
	}

	@Override
	public String getOauthClientSecret() {
		return configFragment.getOauthClientSecret();
	}

	@Override
	public String getSalesforceUri() {
		return configFragment.getSalesforceUri();
	}

	@Override
	public String getSalesforceApiVersion() {
		return configFragment.getSalesforceApiVersion();
	}

	@Override
	public int getSalesforceMaxRecords() {
		return configFragment.getSalesforceMaxRecords();
	}

	@Override
	public String getSalesforceOauthUri() {
		return configFragment.getSalesforceOauthUri();
	}

}
