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
package io.aiven.kafka.connect.salesforce.sink;

import static org.assertj.core.api.Assertions.assertThat;

import io.aiven.kafka.connect.salesforce.sink.config.SalesforceSinkConfigDef;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SalesforceSinkConnector} */
public final class SalesforceSinkConnectorTest {

  @Test
  void testBasic() {
    var connector = new SalesforceSinkConnector();
    // Just check that a version is found
    assertThat(SalesforceSinkConnector.VERSION).isNotEqualTo("unknown");
    assertThat(connector.version()).isSameAs(SalesforceSinkConnector.VERSION);

    // Check the types of configuration and tasks
    assertThat(connector.config()).isInstanceOf(SalesforceSinkConfigDef.class);
    assertThat(connector.taskClass()).isEqualTo(SalesforceSinkTask.class);
  }

  @Test
  void testTaskConfigs() {
    var connector = new SalesforceSinkConnector();

    connector.start(Map.of("key", "value"));
    var configs = connector.taskConfigs(2);
    assertThat(configs.size()).isEqualTo(2);

    for (int i = 0; i < configs.size(); i++) {
      var cfg = configs.get(i);
      assertThat(cfg).containsEntry("key", "value");
      assertThat(cfg).containsEntry("task.id", String.valueOf(i));
    }
  }
}
