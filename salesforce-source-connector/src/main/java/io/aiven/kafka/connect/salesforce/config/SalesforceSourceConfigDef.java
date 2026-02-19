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

import io.aiven.commons.kafka.config.CommonConfigDef;
import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import org.apache.kafka.common.config.ConfigValue;

import java.util.Map;

/**
 * Common ConfigDef for Salesforce
 */
public class SalesforceSourceConfigDef extends CommonConfigDef {

	/**
	 * Default constructor
	 */
	public SalesforceSourceConfigDef() {
		super();
		// ensure that we have the properties from the config fragment.
		SalesforceConfigFragment.update(this);
	}

	/**
	 * Validates the Salesforce configuration is correct and meets requirements
	 *
	 * @param valueMap
	 *            the map of configuration names to values.
	 * @return the updated map.
	 */
	@Override
	public Map<String, ConfigValue> multiValidate(final Map<String, ConfigValue> valueMap) {
		Map<String, ConfigValue> values = super.multiValidate(valueMap);
		// validate that the config fragment options are good.
		FragmentDataAccess fragmentDataAccess = FragmentDataAccess.from(valueMap);
		new SalesforceConfigFragment(fragmentDataAccess).validate(values);
		return values;
	}

}
