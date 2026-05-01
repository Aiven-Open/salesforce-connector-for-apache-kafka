/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.aiven.kafka.connect.salesforce.sink.config;

import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.commons.kafka.connector.common.config.ConnectorCommonConfigDef;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

/** The configuration specification for the Salesforce sink */
public final class SalesforceSinkConfigDef extends ConnectorCommonConfigDef {

  /** Default constructor */
  public SalesforceSinkConfigDef() {
    super();
    SalesforceCommonConfigFragment.update(this);
    SalesforceSinkConfigFragment.update(this);
  }

  /**
   * Validates the Salesforce configuration is correct and meets requirements
   *
   * @param valueMap the map of configuration names to values.
   * @return the updated map.
   */
  @Override
  public Map<String, ConfigValue> multiValidate(final Map<String, ConfigValue> valueMap) {
    Map<String, ConfigValue> values = super.multiValidate(valueMap);
    // validate that the config fragment options are good.
    FragmentDataAccess fragmentDataAccess = FragmentDataAccess.from(valueMap);
    new SalesforceCommonConfigFragment(fragmentDataAccess).validate(values);
    new SalesforceSinkConfigFragment(fragmentDataAccess).validate(values);
    return values;
  }
}
