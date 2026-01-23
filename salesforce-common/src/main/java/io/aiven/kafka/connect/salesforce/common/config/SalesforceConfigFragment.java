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

import io.aiven.commons.kafka.config.ExtendedConfigKey;

import io.aiven.commons.kafka.config.fragment.AbstractFragmentSetter;
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
	private static final String SALESFORCE_API_VERSION_DEFAULT = "v66.0";

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
	 * The prefix used to determine the topic names to send the events. Events will
	 * be sent to topics with topic_prefix.[api_name].[object_name]
	 */
	public static final String TOPIC_PREFIX = "topic.prefix";

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
	 * Setter to configure configData
	 * 
	 * @param configData
	 *            ConfigData is the map of properties and values to update
	 * @return The Setter for the configData
	 */
	public static Setter setter(final Map<String, String> configData) {
		return new Setter(configData);
	}

	/**
	 * Add the salesforce connection configuration required to the configDef
	 * 
	 * @param configDef
	 *            Add the Salesforce connection configuration required to the
	 *            configDef
	 */
	static void addSalesforceConnectionDetails(final ConfigDef configDef) {
		var salesforceGroupCounter = 0;
		configDef.define(ExtendedConfigKey.builder(SALESFORCE_MAX_RECORDS).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").defaultValue(SALESFORCE_MAX_RECORDS_DEFAULT)
				.type(ConfigDef.Type.LONG).validator(ConfigDef.Range.between(1L, 15000L))
				.importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"Salesforce default maximum number of records to retrieve from the Bulk API. Must be at least 100 and at most 100000, default value is "
								+ SALESFORCE_MAX_RECORDS_DEFAULT)
				.width(ConfigDef.Width.NONE).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_MAX_RETRIES).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").defaultValue(SALESFORCE_MAX_RETRIES_DEFAULT)
				.type(ConfigDef.Type.LONG).validator(ConfigDef.Range.between(1L, 5L))
				.importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"Salesforce default maximum number of retries against API. Must be at least 1 and at most 5, default value is "
								+ SALESFORCE_MAX_RETRIES_DEFAULT)
				.width(ConfigDef.Width.NONE).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_API_VERSION).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").defaultValue(SALESFORCE_API_VERSION_DEFAULT)
				.type(ConfigDef.Type.STRING).validator(new ConfigDef.NonEmptyString())
				.importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"API version of the Salesforce API to use when communicating with Salesforce, default value is "
								+ SALESFORCE_API_VERSION_DEFAULT)
				.width(ConfigDef.Width.NONE).build());

		// Salesforce authentication config
		configDef.define(ExtendedConfigKey.builder(SALESFORCE_USERNAME).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").type(ConfigDef.Type.STRING)
				.validator(new ConfigDef.NonEmptyString()).importance(ConfigDef.Importance.MEDIUM)
				.documentation("Salesforce username that is used to authenticate over oauth with the api.")
				.width(ConfigDef.Width.NONE).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_PASSWORD).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").type(ConfigDef.Type.STRING)
				.validator(new ConfigDef.NonEmptyString()).importance(ConfigDef.Importance.MEDIUM)
				.documentation("Salesforce password that is used to authenticate over oauth with the api.")
				.width(ConfigDef.Width.NONE).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_CLIENT_ID).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").type(ConfigDef.Type.STRING)
				.validator(new ConfigDef.NonEmptyString()).importance(ConfigDef.Importance.MEDIUM)
				.documentation("Salesforce client id that is used to authenticate over oauth with the api.")
				.width(ConfigDef.Width.NONE).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_CLIENT_SECRET).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").type(ConfigDef.Type.STRING)
				.validator(new ConfigDef.NonEmptyString()).importance(ConfigDef.Importance.MEDIUM)
				.documentation("Salesforce client secret that is used to authenticate over oauth with the api.")
				.width(ConfigDef.Width.NONE).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_URI).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").type(ConfigDef.Type.STRING)
				.validator(new ConfigDef.NonEmptyString()).importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"Salesforce domain uri that is used to query the bulk api this is a uri specific to your organization and domain supplied by Salesforce.")
				.width(ConfigDef.Width.NONE).build());
		configDef.define(ExtendedConfigKey.builder(SALESFORCE_OAUTH_URI).group(GROUP_SALESFORCE)
				.orderInGroup(++salesforceGroupCounter).since("1.0.0").type(ConfigDef.Type.STRING)
				.validator(new ConfigDef.NonEmptyString()).importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"Salesforce oauth uri that is used to authenticate over oauth with the api, this is a uri specific to your organization and domain supplied by Salesforce.")
				.width(ConfigDef.Width.NONE).build());

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
	 * Client Id used for Oauth configuration Also called the Client Key in
	 * Salesforce
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

	/**
	 * 
	 * The maximum number of retries to execute when making queries against the Bulk
	 * API
	 * 
	 * @return An int that is the number of retries to allow
	 */
	public int getSalesforceMaxRetries() {
		return dataAccess.getInt(SALESFORCE_MAX_RETRIES);
	}

	/**
	 * A setter for the SalesforceConfigFragment.
	 */
	public final static class Setter extends AbstractFragmentSetter<Setter> {

		private Setter(final Map<String, String> data) {
			super(data);
		}

		/**
		 * Set the Username used for Oauth configuration
		 *
		 * @param oAuthUserName
		 *            The username used for Oauth authentication
		 * @return The Oauth Salesforce username
		 */
		public Setter getOauthUsername(String oAuthUserName) {
			return setValue(SALESFORCE_USERNAME, oAuthUserName);
		}

		/**
		 * Set the Password used for Oauth configuration
		 *
		 * @param oAuthPassword
		 *            The password used for Oauth authentication
		 * @return The Oauth Salesforce password
		 */
		public Setter getOauthPassword(String oAuthPassword) {
			return setValue(SALESFORCE_PASSWORD, oAuthPassword);
		}

		/**
		 * Set the Client Id used for Oauth configuration
		 *
		 * @param clientId
		 *            The clientId used for Oauth configuration
		 * @return The Oauth Salesforce client Id
		 */
		public Setter getOauthClientId(String clientId) {
			return setValue(SALESFORCE_CLIENT_ID, clientId);
		}

		/**
		 * Set the Client Secret used for Oauth configuration
		 *
		 * @param clientSecret
		 *            the client secret used for authentication
		 * @return The Oauth Salesforce client secret
		 */
		public Setter getOauthClientSecret(String clientSecret) {
			return setValue(SALESFORCE_CLIENT_SECRET, clientSecret);
		}

		/**
		 * Set the specific Salesforce uri used for all requests to the bulk api
		 *
		 * @param salesforceUri
		 *            A string representation of the uri to use with Salesforce
		 * @return The target Salesforce Uri
		 */
		public Setter getSalesforceUri(String salesforceUri) {
			return setValue(SALESFORCE_URI, salesforceUri);
		}

		/**
		 * Set the Salesforce Api version to be returned
		 *
		 * @param apiVersion
		 *            A string that identifies the Salesforce apiVersion that the
		 *            connector should execute against
		 * @return The target Salesforce api version
		 */
		public Setter getSalesforceApiVersion(String apiVersion) {
			return setValue(SALESFORCE_API_VERSION, apiVersion);
		}

		/**
		 * Set the maximum number of records to return from the Bulk Api Query at a
		 * time.
		 *
		 * @param maxRecords
		 *            An int representing the maximum number of records to retrieve from
		 *            Salesforce at a time
		 * @return An int with the maximum number of records to retrieve on each page of
		 *         the Bulk Api.
		 */
		public Setter getSalesforceMaxRecords(int maxRecords) {
			return setValue(SALESFORCE_MAX_RECORDS, maxRecords);
		}

		/**
		 * Set the specific Salesforce uri used for all requests including
		 * authentication and submitting queries
		 *
		 * @param salesforceOauthUri
		 *            A string representation of the oauth uri to use with Salesforce
		 * @return The target Salesforce OAUTH Uri
		 */
		public Setter getSalesforceOauthUri(String salesforceOauthUri) {
			return setValue(SALESFORCE_OAUTH_URI, salesforceOauthUri);
		}

	}
}