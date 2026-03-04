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
package io.aiven.kafka.connect.salesforce.sink.config;

import io.aiven.commons.kafka.config.ExtendedConfigKey;
import io.aiven.commons.kafka.config.SinceInfo;
import io.aiven.commons.kafka.config.fragment.AbstractFragmentSetter;
import io.aiven.commons.kafka.config.fragment.ConfigFragment;
import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

/**
 * A fragment defining the configuration specific to the Salesforce sink
 * connector.
 */
public final class SalesforceSinkConfigFragment extends ConfigFragment {

	/** The user-facing name of the config group for this fragment. */
	private static final String GROUP = "Salesforce Sink";

	/**
	 * The destination object type that will be written to Salesforce (such as
	 * Account or Contact)
	 */
	private static final String SALESFORCE_SINK_OBJECT = "salesforce.bulkapi.sink.object";

	/**
	 * Constructor.
	 * 
	 * @param dataAccess
	 *            the data access for this fragment.
	 */
	SalesforceSinkConfigFragment(FragmentDataAccess dataAccess) {
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
	 * Update the configuration with the options for the Salesforce sink connector.
	 * 
	 * @param configDef
	 *            the configuration definition to update.
	 */
	public static void update(ConfigDef configDef) {
		int groupOrder = 0;
		SinceInfo.Builder siBuilder = SinceInfo.builder().groupId("io.aiven.kafka.connect")
				.artifactId("salesforce-sink-connector");
		configDef.define(ExtendedConfigKey.builder(SALESFORCE_SINK_OBJECT).group(GROUP).orderInGroup(++groupOrder)
				.since(siBuilder.version("1.0.0").build().setVersionOnly()).type(ConfigDef.Type.STRING)
				.importance(ConfigDef.Importance.MEDIUM)
				.documentation(
						"The destination object type that will be written to Salesforce (such as Account or Contact)")
				.width(ConfigDef.Width.MEDIUM).build());
	}

	/**
	 * Gets the destination object type that will be written to Salesforce (such as
	 * Account or Contact).
	 *
	 * @return The destination object type to be written to Salesforce.
	 */
	public String getSinkObject() {
		return dataAccess.getString(SALESFORCE_SINK_OBJECT);
	}

	/**
	 * The setter for the Salesforce sink config.
	 */
	public static final class Setter extends AbstractFragmentSetter<Setter> {
		private Setter(Map<String, String> data) {
			super(data);
		}

		/**
		 * Sets the destination object type that will be written to Salesforce (such as
		 * Account or Contact).
		 *
		 * @param sinkObject
		 *            The destination object type to be written to Salesforce.
		 * @return this
		 */
		public Setter sinkObject(String sinkObject) {
			return setValue(SALESFORCE_SINK_OBJECT, sinkObject);
		}
	}
}
