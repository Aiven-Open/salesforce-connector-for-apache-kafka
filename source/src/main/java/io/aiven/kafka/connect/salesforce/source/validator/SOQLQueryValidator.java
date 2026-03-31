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
package io.aiven.kafka.connect.salesforce.source.validator;

import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import java.util.Objects;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

/** A Config validator for the SOQL validators */
public class SOQLQueryValidator implements ConfigDef.Validator {

  /** default constructor */
  public SOQLQueryValidator() {
    // default constructor
  }

  /**
   * Validates the list of SOQL queries
   *
   * @param name The name of the config
   * @param value The value provided by the user that is to be validated
   */
  @Override
  public void ensureValid(String name, Object value) {
    String valueStr = (String) value;
    if (Objects.nonNull(valueStr) && !valueStr.isEmpty()) {
      var valueStrArray = valueStr.split(";");

      for (String queryString : valueStrArray) {
        if (!SOQLQuery.fromQueryString(queryString).validate()) {
          throw new ConfigException(
              String.format(
                  "%s : %s requires the SELECT statement to have LastModifiedDate specified, it must also not specify the LastModifiedDate in the WHERE clause.",
                  name, queryString));
        }
      }
    } else {
      throw new ConfigException(name, value, "A SOQL query must be defined");
    }
  }

  @Override
  public String toString() {
    return "A valid SOQL Query. Requires the SELECT statement to have LastModifiedDate specified, it must also not specify the LastModifiedDate in the WHERE clause.";
  }
}
