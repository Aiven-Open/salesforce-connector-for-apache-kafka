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
package io.aiven.kafka.connect.salesforce.common.utils;

import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.commons.kafka.connector.common.config.ConnectorCommonConfig;
import io.aiven.commons.kafka.connector.common.config.ConnectorCommonConfigDef;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfig;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;

import java.util.Map;

public class TestingSalesforceCommonConfig extends ConnectorCommonConfig implements SalesforceCommonConfig {

	final SalesforceCommonConfigFragment fragment;

	private static ConnectorCommonConfigDef mkDef() {
		ConnectorCommonConfigDef configDef = new ConnectorCommonConfigDef();
		SalesforceCommonConfigFragment.update(configDef);
		return configDef;
	}
	public TestingSalesforceCommonConfig(Map<String, String> props) {
		super(mkDef(), props);
		FragmentDataAccess dataAccess = FragmentDataAccess.from(this);
		fragment = new SalesforceCommonConfigFragment(dataAccess);
	}

	@Override
	public String getOauthClientId() {
		return fragment.getOauthClientId();
	}

	@Override
	public String getOauthClientSecret() {
		return fragment.getOauthClientSecret();
	}

	@Override
	public String getSalesforceUri() {
		return fragment.getSalesforceUri();
	}

	@Override
	public String getSalesforceApiVersion() {
		return fragment.getSalesforceApiVersion();
	}

	@Override
	public int getSalesforceMaxRecords() {
		return fragment.getSalesforceMaxRecords();
	}

	@Override
	public String getSalesforceOauthUri() {
		return fragment.getSalesforceOauthUri();
	}

	@Override
	public String getTopicPrefix() {
		return fragment.getTopicPrefix();
	}

	@Override
	public int getSalesforceMaxRetries() {
		return fragment.getSalesforceMaxRetries();
	}
}
