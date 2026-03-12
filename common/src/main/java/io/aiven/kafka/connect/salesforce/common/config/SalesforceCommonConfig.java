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

import java.time.Duration;

/**
 * Common methods for Authentication and communication with Salesforce
 */
public interface SalesforceCommonConfig {

	/**
	 * Client Id used for Oauth configuration
	 *
	 * @return The Oauth Salesforce client Id
	 */
	String getOauthClientId();

	/**
	 * Client Secret used for Oauth configuration
	 *
	 * @return The Oauth Salesforce client secret
	 */
	String getOauthClientSecret();

	/**
	 * The specific Salesforce uri used for all requests to the bulk api
	 *
	 * @return The target Salesforce Uri
	 */
	String getSalesforceUri();

	/**
	 * The Salesforce Api version to be returned
	 *
	 * @return The target salesforce api version
	 */
	String getSalesforceApiVersion();

	/**
	 * The maximum number of records to return from the Bulk Api Query at a time.
	 *
	 * @return An int with the maximum number of records to retrieve on each page of
	 *         the Bulk Api.
	 */
	int getSalesforceMaxRecords();

	/**
	 * The specific Salesforce uri used for all requests including authentication
	 * and submitting queries
	 *
	 * @return The target Salesforce OAUTH Uri
	 */
	String getSalesforceOauthUri();

	/**
	 * Gets the topic prefix to be added to calculated topic name.
	 * 
	 * @return the topic prefix TODO Should be replaced with templated topic in
	 *         framework when available.
	 */
	String getTopicPrefix();

	/**
	 * The maximum number of retries for sales force connection.
	 * 
	 * @return the maximum number of retries.
	 */
	int getSalesforceMaxRetries();

	/**
	 * The time to wait in between querying the status of a job
	 *
	 * @return The time in seconds to wait between checking for the status of a bulk
	 *         api job.
	 */
	Duration getStatusCheckWaitTime();

	/**
	 * The minimum time between resubmitting the same SOQL query to the bulk api
	 * 
	 * @return The minimum time in seconds to wait between resubmitting the same
	 *         SOQL query
	 */
	Duration getMinimumQueryExecutionDelay();

}
