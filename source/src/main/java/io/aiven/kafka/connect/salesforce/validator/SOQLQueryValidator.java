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
package io.aiven.kafka.connect.salesforce.validator;

import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Objects;

/**
 * A Config validator for the SOQL validators
 */
public class SOQLQueryValidator implements ConfigDef.Validator {

	/**
	 * default constructor
	 */
	public SOQLQueryValidator() {
		// default constructor
	}

	/**
	 * Validates the list of SOQL queries
	 * 
	 * @param name
	 *            The name of the config
	 * @param value
	 *            The value provided by the user that is to be validated
	 */
	@Override
	public void ensureValid(String name, Object value) {

		if (Objects.nonNull(value)) {
			var valueStr = (String) value;
			if (valueStr.isEmpty()) {
				throw new ConfigException(name, value, "A SOQL query must be defined");
			}

			var valueStrArray = valueStr.split(";");

			for (String queryString : valueStrArray) {
				if (!SOQLQuery.fromQueryString(queryString).validate()) {
					throw new ConfigException(String.format(
							"%s : %s requires the FIELDS(ALL) or the Id and LastModifiedDate in the select statement and the where statement should not have LastModifiedDate specified",
							name, queryString));
				}
			}
		}
	}

	@Override
	public String toString() {
		return "A valid SOQL Query. Requires the SELECT statement to have either FIELDS(ALL) or the Id and LastModifiedDate specified, it must also not specify the LastModifiedDate in the WHERE clause.";
	}
}
