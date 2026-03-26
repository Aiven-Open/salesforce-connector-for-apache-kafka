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
import io.aiven.commons.kafka.config.SinceInfo;
import io.aiven.commons.kafka.config.fragment.AbstractFragmentSetter;
import io.aiven.commons.kafka.config.fragment.ConfigFragment;
import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;

/**
 * The SalesforceCommonConfigFragment has all the configuration needed to authenticate and
 * communicate with a configured Salesforce system. It makes for easy access and passing of the
 * configuration securely.
 */
public final class SalesforceCommonConfigFragment extends ConfigFragment {

  /**
   * The prefix used to determine the topic names to send the events. Events will be sent to topics
   * with topic_prefix.[api_name].[object_name]
   */
  private static final String TOPIC_PREFIX =
      "topic.prefix"; // NOPMD will be used v shortly to put records on topics

  /** Group name for the salesforce config */
  private static final String GROUP_SALESFORCE = "Salesforce";

  /** The maximum number of records that can be retrieved from a salesforce bulk query at a time. */
  private static final String SALESFORCE_MAX_RECORDS = "salesforce.max.records";

  /** The default maximum number of records that can be retrieved from the bulk api in one go */
  private static final int SALESFORCE_MAX_RECORDS_DEFAULT = 50000;

  /** The maximum number of records that can be retrieved from a salesforce bulk query at a time. */
  private static final String SALESFORCE_MAX_RETRIES = "salesforce.max.retries";

  /** The default maximum number of records that can be retrieved from the bulk api in one go */
  private static final int SALESFORCE_MAX_RETRIES_DEFAULT = 3;

  /** The version of the API that the connector should use to run its queries */
  private static final String SALESFORCE_API_VERSION = "salesforce.api.version";

  private static final String SALESFORCE_API_VERSION_DEFAULT = "v65.0";

  /** The salesforce client secret for authentication */
  private static final String SALESFORCE_CLIENT_SECRET = "salesforce.client.secret";

  /** The salesforce client id for authentication */
  private static final String SALESFORCE_CLIENT_ID = "salesforce.client.id";

  /** The wait time in between api calls to check a bulk api job status in seconds */
  private static final String SALESFORCE_STATUS_CHECK_WAIT = "salesforce.status.check.wait";

  /** The wait time in between api calls to check a bulk api job status in seconds */
  private static final Duration SALESFORCE_STATUS_CHECK_WAIT_DEFAULT = Duration.ofSeconds(5);

  /** The wait between executing the same SOQL query again in seconds */
  private static final String SALESFORCE_WAIT_BETWEEN_QUERIES = "salesforce.soql.query.wait";

  /** The default wait between executing the same SOQL query again in seconds */
  private static final Duration SALESFORCE_WAIT_BETWEEN_QUERIES_DEFAULT = Duration.ofSeconds(300);

  /** The salesforce organization uri for Bulk Api and pub sub queries */
  private static final String SALESFORCE_URI = "salesforce.uri";

  /** The salesforce OAUTH organization uri for password */
  private static final String SALESFORCE_OAUTH_URI = "salesforce.oauth.uri";

  /**
   * Allows data to be added directly into the config fragment
   *
   * @param dataAccess A FragmentDataAccess with corresponding config
   */
  public SalesforceCommonConfigFragment(FragmentDataAccess dataAccess) {
    super(dataAccess);
  }

  /**
   * Override of the validate method
   *
   * @param configMap The map of all values for configuration
   */
  @Override
  public void validate(Map<String, ConfigValue> configMap) { // NOPMD
    super.validate(configMap);
    // useless overriding method ignore as we will add
    // handle any restrictions between options here.
  }

  /**
   * Setter to configure configData
   *
   * @param configData ConfigData is the map of properties and values to update
   * @return The Setter for the configData
   */
  public static Setter setter(final Map<String, String> configData) {
    return new Setter(configData);
  }

  /**
   * Adds the configuration options that are common to all Salesforce configurations.
   *
   * @param configDef the Configuration definition.
   */
  public static void update(final ConfigDef configDef) {
    var salesforceGroupCounter = 0;
    SinceInfo.Builder siBuilder =
        SinceInfo.builder()
            .groupId("io.aiven.kafka.connect")
            .artifactId("salesforce-connector-for-kafka-connect");
    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_MAX_RECORDS)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .defaultValue(SALESFORCE_MAX_RECORDS_DEFAULT)
            .type(ConfigDef.Type.INT)
            .validator(ConfigDef.Range.between(1, 150000))
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "Salesforce default maximum number of records to retrieve from the Bulk API. Must be at least 100 and at most 100000, default value is "
                    + SALESFORCE_MAX_RECORDS_DEFAULT)
            .width(ConfigDef.Width.NONE)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_MAX_RETRIES)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .defaultValue(SALESFORCE_MAX_RETRIES_DEFAULT)
            .type(ConfigDef.Type.INT)
            .validator(ConfigDef.Range.between(1, 5))
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "Salesforce default maximum number of retries against API. Must be at least 1 and at most 5, default value is "
                    + SALESFORCE_MAX_RETRIES_DEFAULT)
            .width(ConfigDef.Width.NONE)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_API_VERSION)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .defaultValue(SALESFORCE_API_VERSION_DEFAULT)
            .type(ConfigDef.Type.STRING)
            .validator(new ConfigDef.NonEmptyString())
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "API version of the Salesforce API to use when communicating with Salesforce, default value is "
                    + SALESFORCE_API_VERSION_DEFAULT)
            .width(ConfigDef.Width.NONE)
            .build());

    // Salesforce authentication config
    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_CLIENT_ID)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.PASSWORD)
            .validator(null)
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "Salesforce client id that is used to authenticate over oauth with the api.")
            .width(ConfigDef.Width.NONE)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_CLIENT_SECRET)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.PASSWORD)
            .validator(null)
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "Salesforce client secret that is used to authenticate over oauth with the api.")
            .width(ConfigDef.Width.NONE)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_URI)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.STRING)
            .validator(new ConfigDef.NonEmptyString())
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "Salesforce domain uri that is used to query the bulk api this is a uri specific to your organization and domain supplied by Salesforce.")
            .width(ConfigDef.Width.NONE)
            .build());
    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_OAUTH_URI)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.STRING)
            .validator(new ConfigDef.NonEmptyString())
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                "Salesforce oauth uri that is used to authenticate over oauth with the api, this is a uri specific to your organization and domain supplied by Salesforce.")
            .width(ConfigDef.Width.NONE)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(TOPIC_PREFIX)
            .group(GROUP_SALESFORCE)
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.STRING)
            .importance(ConfigDef.Importance.HIGH)
            .documentation(
                TOPIC_PREFIX
                    + " is a required to determine what topic to put the events onto, the prefix is used as `<topic.prefix>.<api_name>.<object_name>` for example if set to history then events from the Account table from "
                    + "the bulk api will be produced to history.bulkApi.Account.")
            .width(ConfigDef.Width.LONG)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_WAIT_BETWEEN_QUERIES)
            .group(GROUP_SALESFORCE)
            .defaultValue(SALESFORCE_WAIT_BETWEEN_QUERIES_DEFAULT.getSeconds())
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.LONG)
            .validator(ConfigDef.Range.between(1, 604800))
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                SALESFORCE_WAIT_BETWEEN_QUERIES
                    + " allows a user to configure the minimum time in seconds between re-executing the same SOQL query against the API the default value is "
                    + SALESFORCE_WAIT_BETWEEN_QUERIES_DEFAULT.getSeconds()
                    + " seconds. Minimum 1 second delay and a maximum of 604800 seconds or one week.")
            .width(ConfigDef.Width.LONG)
            .build());

    configDef.define(
        ExtendedConfigKey.builder(SALESFORCE_STATUS_CHECK_WAIT)
            .group(GROUP_SALESFORCE)
            .defaultValue(SALESFORCE_STATUS_CHECK_WAIT_DEFAULT.toSeconds())
            .orderInGroup(++salesforceGroupCounter)
            .since(siBuilder.version("1.0.0").build())
            .type(ConfigDef.Type.LONG)
            .validator(ConfigDef.Range.between(5, 3600))
            .importance(ConfigDef.Importance.MEDIUM)
            .documentation(
                SALESFORCE_STATUS_CHECK_WAIT
                    + " allows a user to configure the time in seconds between individual api calls to check the status of a bulk job. Each SOQL query requires 1 or many calls to see if the job is ready to be processed, this configuration allows the user to reduce or increase the number of calls made to the api to check the status. "
                    + SALESFORCE_WAIT_BETWEEN_QUERIES_DEFAULT.toSeconds()
                    + " seconds. Minimum 5 seconds and maximum 3600 seconds or one hour.")
            .width(ConfigDef.Width.LONG)
            .build());
  }

  /**
   * Client Id used for Oauth configuration Also called the Client Key in Salesforce
   *
   * @return The Oauth Salesforce client Id
   */
  public String getOauthClientId() {
    return dataAccess.getPassword(SALESFORCE_CLIENT_ID).value();
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
   * @return An int with the maximum number of records to retrieve on each page of the Bulk Api.
   */
  public int getSalesforceMaxRecords() {
    return dataAccess.getInt(SALESFORCE_MAX_RECORDS);
  }

  /**
   * The specific Salesforce uri used for all requests including authentication and submitting
   * queries
   *
   * @return The target Salesforce OAUTH Uri
   */
  public String getSalesforceOauthUri() {
    return dataAccess.getString(SALESFORCE_OAUTH_URI);
  }

  /**
   * The maximum number of retries to execute when making queries against the Bulk API
   *
   * @return An int that is the number of retries to allow
   */
  public int getSalesforceMaxRetries() {
    return dataAccess.getInt(SALESFORCE_MAX_RETRIES);
  }

  /**
   * Get the topic prefix which is used to determine the topic names that data is sent to.
   *
   * @return The topic prefix to send data to.
   */
  public String getTopicPrefix() {
    return dataAccess.getString(TOPIC_PREFIX);
  }

  /**
   * The minimum time to wait between the submitting of the same SOQL query
   *
   * @return The time in seconds to wait between submitting the same SOQL query
   */
  public Duration getSalesforceWaitBetweenQueries() {
    return Duration.ofSeconds(getLong(SALESFORCE_WAIT_BETWEEN_QUERIES));
  }

  /**
   * The time to wait between checking the status of a job that has been submitted to the bulk api
   * in seconds
   *
   * @return the time in seconds to wait between checking the status of a job
   */
  public Duration getSalesforceStatusCheckWait() {
    return Duration.ofSeconds(getLong(SALESFORCE_STATUS_CHECK_WAIT));
  }

  /** A setter for the SalesforceCommonConfigFragment. */
  public static final class Setter extends AbstractFragmentSetter<Setter> {

    private Setter(final Map<String, String> data) {
      super(data);
    }

    /**
     * Set the Client Id used for Oauth configuration
     *
     * @param clientId The clientId used for Oauth configuration
     * @return this
     */
    public Setter oauthClientId(String clientId) {
      return setValue(SALESFORCE_CLIENT_ID, clientId);
    }

    /**
     * Set the Client Secret used for Oauth configuration
     *
     * @param clientSecret the client secret used for authentication
     * @return this
     */
    public Setter oauthClientSecret(String clientSecret) {
      return setValue(SALESFORCE_CLIENT_SECRET, clientSecret);
    }

    /**
     * Set the specific Salesforce uri used for all requests to the bulk api
     *
     * @param salesforceUri A string representation of the uri to use with Salesforce
     * @return The target Salesforce Uri
     */
    public Setter uri(String salesforceUri) {
      return setValue(SALESFORCE_URI, salesforceUri);
    }

    /**
     * Set the Salesforce Api version to be returned
     *
     * @param apiVersion A string that identifies the Salesforce apiVersion that the connector
     *     should execute against
     * @return this
     */
    public Setter apiVersion(String apiVersion) {
      return setValue(SALESFORCE_API_VERSION, apiVersion);
    }

    /**
     * Set the maximum number of records to return from the Bulk Api Query at a time.
     *
     * @param maxRecords An int representing the maximum number of records to retrieve from
     *     Salesforce at a time
     * @return this
     */
    public Setter maxRecords(int maxRecords) {
      return setValue(SALESFORCE_MAX_RECORDS, maxRecords);
    }

    /**
     * Set the specific Salesforce uri used for all requests including authentication and submitting
     * queries
     *
     * @param salesforceOauthUri A string representation of the oauth uri to use with Salesforce
     * @return this
     */
    public Setter oauthUri(String salesforceOauthUri) {
      return setValue(SALESFORCE_OAUTH_URI, salesforceOauthUri);
    }

    /**
     * Sets the topic prefix which determines the name of the topic data is sent to.
     *
     * @param topicPrefix the topic prefix
     * @return this
     */
    public Setter topicPrefix(String topicPrefix) {
      return setValue(TOPIC_PREFIX, topicPrefix);
    }

    /**
     * Sets the maximum retries for connections to Salesforce.
     *
     * @param maxRetries the maximum retries for connections.
     * @return this
     */
    public Setter maxRetries(int maxRetries) {
      return setValue(SALESFORCE_MAX_RETRIES, maxRetries);
    }
  }
}
