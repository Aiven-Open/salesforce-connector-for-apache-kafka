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
package io.aiven.kafka.connect.salesforce.source.config;

import io.aiven.commons.kafka.config.fragment.FragmentDataAccess;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

/** Source ConfigDef for Salesforce */
public final class SalesforceSourceConfigDef extends SourceCommonConfig.SourceCommonConfigDef {

  /** Default constructor */
  public SalesforceSourceConfigDef() {
    super();
    SalesforceCommonConfigFragment.update(this);
    SalesforceSourceConfigFragment.update(this);
    hideDocumentation();
  }

  /** Hide all the documentation that the user doesn't need to see */
  protected void hideDocumentation() {
    hideCompressionType(true);
    hideDistributionType(true);
    hideExtractorBuffer(true);
    hideExtractorCacheSize(true);
    hideExtractorCSVHeaders(true);
    hideExtractorCSVHeadersEnabled(true);
    hideExtractorExtractorClass(true);
    hide("kafka.retry.backoff.ms", true);
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
    new SalesforceSourceConfigFragment(fragmentDataAccess).validate(values);
    return values;
  }
}
