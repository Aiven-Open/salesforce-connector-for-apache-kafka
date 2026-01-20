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
package io.aiven.kafka.connect.salesforce.common.config;

import io.aiven.commons.kafka.config.CommonConfig;

import java.util.Map;

/**
 * Salesforce Common Config to instantiate all the required configuration for
 * the salesforce connector that is common to a source and a sink connector.
 */
public class SalesforceCommonConfig extends CommonConfig {

	/**
	 * Instantiation of the SalesforceCommonConfig class
	 * 
	 * @param definition
	 *            The Common ConfigDef implementation that has all the configuration
	 * @param originals
	 *            The original configuration that is stored in a Map of String,
	 *            String
	 */
	public SalesforceCommonConfig(CommonConfigDef definition, Map<String, String> originals) { // NOPMD
		super(definition, originals);
		validate(); // NOPMD ConstructorCallsOverridableMethod
	}

	private void validate() {

	}

}
