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

import io.aiven.commons.kafka.config.ExtendedConfigKey;
import io.aiven.commons.kafka.config.SinceInfo;
import io.aiven.commons.kafka.config.fragment.AbstractFragmentSetter;
import io.aiven.commons.kafka.config.fragment.ConfigFragment;
import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.kafka.connect.salesforce.validator.SOQLQueryValidator;
import org.apache.kafka.common.config.ConfigDef;

import java.util.List;
import java.util.Map;

/**
 * A fragment defining the configuration options specific to Salesforce Source
 * connector.
 */
public final class SalesforceSourceConfigFragment extends ConfigFragment {

	/**
	 * The String list of queries separated by a semicolon to distinguish between
	 * individual queries, it can be null if not sing the bulk api
	 */
	private static final String SALESFORCE_BULK_API_QUERIES = "salesforce.bulk.api.queries";
	/**
	 * The last modified start time to start returning records from the salesforce
	 * object
	 */
	private static final String SALESFORCE_LAST_MODIFIED_START_DATE = "salesforce.lastModifiedStartDate";

	private static final String group = "Salesforce Source";
	/**
	 * Constructor.
	 * 
	 * @param dataAccess
	 *            the data access for this fragment.
	 */
	SalesforceSourceConfigFragment(FragmentDataAccess dataAccess) {
		super(dataAccess);
	}

	/**
	 * Gets the setter for this fragment.
	 * 
	 * @param props
	 *            the properties to be updated.
	 * @return the Setter.
	 */
	public static Setter setter(Map<String, String> props) {
		return new Setter(props);
	}

	/**
	 * Update the configuration with the options for the Salesforce source
	 * connector.
	 * 
	 * @param configDef
	 *            the configuration definition to update.
	 */
	public static void update(ConfigDef configDef) {

		int groupOrder = 0;
		SinceInfo.Builder siBuilder = SinceInfo.builder().groupId("io.aiven.kafka.connect")
				.artifactId("salesforce-source-connector");
		configDef.define(ExtendedConfigKey.builder(SALESFORCE_BULK_API_QUERIES).group(group).orderInGroup(++groupOrder)
				.validator(new SOQLQueryValidator()).since(siBuilder.version("1.0.0").build().setVersionOnly())
				.type(ConfigDef.Type.STRING).importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"Salesforce bulk api queries are used to query for large amounts of data using SOQL a query typically looks like `SELECT Id,Name FROM Account` or when querying multiple Objects "
								+ "`SELECT {{Id}},Name, {{LastModifiedDate}} FROM Account ; SELECT Id, FirstName, Name FROM Contact; SELECT LastName__c, FirstName__c, PhoneNumber__c FROM Phone_Book__b`")
				.width(ConfigDef.Width.LONG).build());

		configDef.define(ExtendedConfigKey.builder(SALESFORCE_LAST_MODIFIED_START_DATE).group(group)
				.orderInGroup(++groupOrder).since(siBuilder.version("1.0.0").build()).type(ConfigDef.Type.STRING)
				.importance(ConfigDef.Importance.MEDIUM)
				.documentation(SALESFORCE_LAST_MODIFIED_START_DATE
						+ " allows a user to query data starting from a specific time, reducing the amount of data that is returned by the api. The default behaviour returns all matching entries from the query regardless of its age. Expected format is 00:00:00T")
				.width(ConfigDef.Width.LONG).build());
	}

	/**
	 * All Salesforce SOQL Queries that are to be executed against the Bulk Api
	 *
	 * @return A LinkedList of queries that are to be made against the Bulk Api.
	 *
	 */
	public List<String> getBulkApiQueries() {
		return List.of(dataAccess.getString(SALESFORCE_BULK_API_QUERIES).split(";"));
	}

	/**
	 * Return the lastModifiedDate to be used with queries
	 * 
	 * @return lastModifiedDate as a String
	 */
	public String getSalesforceLastModifiedStartDate() {
		return dataAccess.getString(SALESFORCE_LAST_MODIFIED_START_DATE);
	}

	/**
	 * The setter for the Salesforce Source config.
	 */
	public static final class Setter extends AbstractFragmentSetter<Setter> {
		private Setter(Map<String, String> data) {
			super(data);
		}

		/**
		 * Sets the queries which are to be used against the Bulk Api
		 *
		 * @param salesforceBulkApiQueries
		 *            The queries to be made against the Bulk Api delimitated by a
		 *            semicolon
		 * @return The queries to be made against the Bulk Api delimitate by a semicolon
		 */
		public Setter bulkApiQueries(String salesforceBulkApiQueries) {
			return setValue(SALESFORCE_BULK_API_QUERIES, salesforceBulkApiQueries);
		}
	}
}
