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

import static org.assertj.core.api.Assertions.assertThat;

import io.aiven.commons.kafka.config.docs.ConfigDefBeanFactory;
import io.aiven.commons.kafka.config.docs.ExtendedConfigKeyBean;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.runtime.ConnectorConfig;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SalesforceSinkConfigDef} */
public class SalesforceSinkConfigDefTest {

  @Test
  void sinceTest() {
    Map<String, List<ConfigDef.ConfigKey>> defaultConfigs =
        ConnectorConfig.configDef().configKeys().values().stream()
            .collect(Collectors.groupingBy(key -> key.name));
    for (ExtendedConfigKeyBean bean :
        new ConfigDefBeanFactory().open(SalesforceSinkConfigDef.class.getName()).configKeys()) {
      if (!defaultConfigs.containsKey(bean.getName())) {
        assertThat(bean.since()).as(bean.getName()).containsAnyOf("0.2.0", "Kafka 0.9.0.0");
      }
    }
  }
}
