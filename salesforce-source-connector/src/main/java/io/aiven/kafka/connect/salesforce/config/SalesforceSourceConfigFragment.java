package io.aiven.kafka.connect.salesforce.config;

import io.aiven.commons.kafka.config.ExtendedConfigKey;
import io.aiven.commons.kafka.config.SinceInfo;
import io.aiven.commons.kafka.config.fragment.AbstractFragmentSetter;
import io.aiven.commons.kafka.config.fragment.ConfigFragment;
import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import org.apache.kafka.common.config.ConfigDef;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class SalesforceSourceConfigFragment extends ConfigFragment {

	/**
	 * The String list of queries separated by a semicolon to distinguish between
	 * individual queries, it can be null if not sing the bulk api
	 */
	private static final String SALESFORCE_BULK_API_QUERIES = "salesforce.bulk.api.queries";

	SalesforceSourceConfigFragment(FragmentDataAccess dataAccess) {
		super(dataAccess);
	}

	public static Setter setter(Map<String, String> props) {
		return new Setter(props);
	}

	public static void update(ConfigDef configDef) {
		String group = "SalesForce Source";
		int groupOrder = 0;
		SinceInfo.Builder siBuilder = SinceInfo.builder().groupId("io.aiven.kafka.connect")
				.artifactId("salesforce-source-connector");
		configDef.define(ExtendedConfigKey.builder(SALESFORCE_BULK_API_QUERIES).group(group).orderInGroup(++groupOrder)
				.since(siBuilder.version("1.0.0").build().setVersionOnly()).type(ConfigDef.Type.STRING)
				.importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"Salesforce bulk api queries are used to query for large amounts of data using SOQL a query typically looks like `SELECT Id,Name FROM Account` or when querying multiple Objects "
								+ "`SELECT Id,Name FROM Account; SELECT Id, FirstName, Name FROM Contact; SELECT LastName__c, FirstName__c, PhoneNumber__c FROM Phone_Book__b`")
				.width(ConfigDef.Width.LONG).build());
	}

	/**
	 * All Salesforce SOQL Queries that are to be executed against the Bulk Api
	 *
	 * @return A LinkedList of queries that are to be made against the Bulk Api.
	 *
	 */
	public LinkedList<String> getBulkApiQueries() {
		return new LinkedList<>(List.of(dataAccess.getString(SALESFORCE_BULK_API_QUERIES).split(";")));
	}

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
