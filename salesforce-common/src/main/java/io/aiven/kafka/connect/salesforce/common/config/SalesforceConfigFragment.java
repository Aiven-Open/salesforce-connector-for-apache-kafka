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

import io.aiven.commons.kafka.config.fragment.ConfigFragment;
import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;

import java.util.Map;
/**
 * The SalesforceConfigFragment has all the configuration needed to authenticate
 * and communicate with a configured Salesforce system. It makes for easy access
 * and passing of the configuration securely.
 */
public class SalesforceConfigFragment extends ConfigFragment {

	/**
	 * Group name for the salesforce config
	 */
	private static final String GROUP_SALESFORCE = "Salesforce";

	/**
	 * The maximum number of records that can be retrieved from a salesforce bulk
	 * query at a time.
	 */
	public static final String SALESFORCE_MAX_RECORDS = "max.records";
	/**
	 * The default maximum number of records that can be retrieved from the bulk api
	 * in one go
	 */
	private static final int SALESFORCE_MAX_RECORDS_DEFAULT = 50000;

	/**
	 * The maximum number of records that can be retrieved from a salesforce bulk
	 * query at a time.
	 */
	public static final String SALESFORCE_MAX_RETRIES = "max.retries";
	/**
	 * The default maximum number of records that can be retrieved from the bulk api
	 * in one go
	 */
	private static final int SALESFORCE_MAX_RETRIES_DEFAULT = 3;

	/**
	 * The version of the API that the connector should use to run its queries
	 */
	public static final String SALESFORCE_API_VERSION = "salesforce.api.version";
	private static final String SALESFORCE_API_VERSION_DEFAULT = "v66";

	/**
	 * The salesforce client secret for authentication
	 */
	public static final String SALESFORCE_CLIENT_SECRET = "salesforce.client.secret";

	/**
	 * The salesforce client id for authentication
	 */
	public static final String SALESFORCE_CLIENT_ID = "salesforce.client.id";

	/**
	 * The salesforce username for authentication
	 */
	public static final String SALESFORCE_USERNAME = "salesforce.username";

	/**
	 * The salesforce username for password
	 */
	public static final String SALESFORCE_PASSWORD = "salesforce.password";

	/**
	 * The salesforce organization uri for Bulk Api and pub sub queries
	 */
	public static final String SALESFORCE_URI = "salesforce.uri";

	/**
	 * The salesforce OAUTH organization uri for password
	 */
	public static final String SALESFORCE_OAUTH_URI = "salesforce.oauth.uri";

	/**
	 * Allows data to be added directly into the config fragment
	 * 
	 * @param dataAccess
	 *            A FragmentDataAccess with corresponding config
	 *
	 */
	public SalesforceConfigFragment(FragmentDataAccess dataAccess) {
		super(dataAccess);
	}

	/**
	 * Adds the configuration options for compression to the configuration
	 * definition.
	 *
	 * @param configDef
	 *            the Configuration definition.
	 * @return the update configuration definition
	 */
	public static ConfigDef update(final ConfigDef configDef) {
		// later
		addSalesforceConnectionDetails(configDef);
		return configDef;
	}

	/**
	 * Override of the validate method
	 * 
	 * @param configMap
	 *            The map of all values for configuration
	 */
	@Override
	public void validate(Map<String, ConfigValue> configMap) {// NOPMD useless overriding method ignore as we will add
		super.validate(configMap);
		// handle any restrictions between options here.

	}

	/**
	 *
	 * @param configDef
	 */
	static void addSalesforceConnectionDetails(final ConfigDef configDef) {
		var salesforceGroupCounter = 0;
		configDef.define(SALESFORCE_MAX_RECORDS, ConfigDef.Type.LONG, SALESFORCE_MAX_RECORDS_DEFAULT,
				ConfigDef.Range.between(100L, 15000L), ConfigDef.Importance.MEDIUM,
				"Salesforce default maximum number of records to retrieve from the Bulk API. Must be at least 100 and at most 100000, default value is "
						+ SALESFORCE_MAX_RECORDS_DEFAULT,
				GROUP_SALESFORCE, ++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_MAX_RECORDS);

		configDef.define(SALESFORCE_MAX_RETRIES, ConfigDef.Type.LONG, SALESFORCE_MAX_RETRIES_DEFAULT,
				ConfigDef.Range.between(1L, 5L), ConfigDef.Importance.MEDIUM,
				"Salesforce default maximum number of retries against API. Must be at least 1 and at most 5, default value is "
						+ SALESFORCE_MAX_RETRIES_DEFAULT,
				GROUP_SALESFORCE, ++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_MAX_RECORDS);

		configDef.define(SALESFORCE_API_VERSION, ConfigDef.Type.STRING, SALESFORCE_API_VERSION_DEFAULT, null,
				ConfigDef.Importance.MEDIUM,
				"API version of the Salesforce API to use when communicating with Salesforce, default value is "
						+ SALESFORCE_API_VERSION_DEFAULT,
				GROUP_SALESFORCE, ++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_API_VERSION);

		// Salesforce authentication config
		configDef.define(SALESFORCE_USERNAME, ConfigDef.Type.STRING, null, null, ConfigDef.Importance.MEDIUM,
				"Salesforce username that is used to authenticate over oauth with the api.", GROUP_SALESFORCE,
				++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_USERNAME);

		configDef.define(SALESFORCE_PASSWORD, ConfigDef.Type.PASSWORD, null, null, ConfigDef.Importance.MEDIUM,
				"Salesforce password that is used to authenticate over oauth with the api.", GROUP_SALESFORCE,
				++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_PASSWORD);

		configDef.define(SALESFORCE_CLIENT_ID, ConfigDef.Type.PASSWORD, null, null, ConfigDef.Importance.MEDIUM,
				"Salesforce client id that is used to authenticate over oauth with the api.", GROUP_SALESFORCE,
				++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_CLIENT_ID);

		configDef.define(SALESFORCE_CLIENT_SECRET, ConfigDef.Type.PASSWORD, null, null, ConfigDef.Importance.MEDIUM,
				"Salesforce client secret that is used to authenticate over oauth with the api.", GROUP_SALESFORCE,
				++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_CLIENT_SECRET);

		configDef.define(SALESFORCE_URI, ConfigDef.Type.STRING, null, null, ConfigDef.Importance.MEDIUM,
				"Salesforce domain uri that is used to query the bulk api this is a uri specific to your organization and domain supplied by Salesforce.",
				GROUP_SALESFORCE, ++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_URI);

		configDef.define(SALESFORCE_OAUTH_URI, ConfigDef.Type.STRING, null, null, ConfigDef.Importance.MEDIUM,
				"Salesforce oauth uri that is used to authenticate over oauth with the api, this is a uri specific to your organization and domain supplied by Salesforce.",
				GROUP_SALESFORCE, ++salesforceGroupCounter, ConfigDef.Width.NONE, SALESFORCE_OAUTH_URI);
	}

	/**
	 * Username used for Oauth configuration
	 * 
	 * @return The Oauth Salesforce username
	 */
	public String getOauthUsername() {
		return dataAccess.getString(SALESFORCE_USERNAME);
	}

	/**
	 * Password used for Oauth configuration
	 * 
	 * @return The Oauth Salesforce password
	 */
	public String getOauthPassword() {
		return dataAccess.getPassword(SALESFORCE_PASSWORD).value();
	}

	/**
	 * Client Id used for Oauth configuration
	 * 
	 * @return The Oauth Salesforce client Id
	 */
	public String getOauthClientId() {
		return dataAccess.getString(SALESFORCE_CLIENT_ID);
	}

	/**
	 * Client Secret used for Oauth configuration
	 * 
	 * @return The Oauth Salesforce client secret
	 */
	public String getOauthClientSecret() {
		return dataAccess.getPassword(SALESFORCE_CLIENT_SECRET).value();
	}

	/**
	 * The specific Salesforce uri used for all requests to the bulk api
	 * 
	 * @return The target Salesforce Uri
	 */
	public String getSalesforceUri() {
		return dataAccess.getString(SALESFORCE_URI);
	}

	/**
	 * The Salesforce Api version to be returned
	 * 
	 * @return The target salesforce api version
	 */
	public String getSalesforceApiVersion() {
		return dataAccess.getString(SALESFORCE_API_VERSION);
	}

	/**
	 * The maximum number of records to return from the Bulk Api Query at a time.
	 * 
	 * @return An int with the maximum number of records to retrieve on each page of
	 *         the Bulk Api.
	 */
	public int getSalesforceMaxRecords() {
		return dataAccess.getInt(SALESFORCE_MAX_RECORDS);
	}

	/**
	 * The specific Salesforce uri used for all requests including authentication
	 * and submitting queries
	 *
	 * @return The target Salesforce OAUTH Uri
	 */
	public String getSalesforceOauthUri() {
		return dataAccess.getString(SALESFORCE_OAUTH_URI);
	}

}