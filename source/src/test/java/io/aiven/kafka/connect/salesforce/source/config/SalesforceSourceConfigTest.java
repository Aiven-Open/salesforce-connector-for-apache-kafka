package io.aiven.kafka.connect.salesforce.source.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

public class SalesforceSourceConfigTest {

  @Test
  void defaultValues() {
    final var props = new HashMap<String, String>();

    assertThatThrownBy(() -> new SalesforceSourceConfig(props))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("Missing required configuration ")
        .hasMessageContaining("salesforce.bulk.api.queries")
        .hasMessageContaining(" which has no default value.");
    SalesforceSourceConfigFragment.setter(props)
        .bulkApiQueries("SELECT Id,LastModifiedDate FROM Account");

    SalesforceSourceConfig config = new SalesforceSourceConfig(props);
    assertEquals(config.getSalesforceApiVersion(), "v65.0");
    assertEquals(config.getSalesforceOauthUri(), null);
    assertEquals(config.getLastModifiedStartDateTime(), null);
    assertEquals(config.getStatusCheckWaitTime(), Duration.ofSeconds(5));
    assertEquals(config.getMinimumQueryExecutionDelay(), Duration.ofSeconds(300));
    assertEquals(config.getSalesforceMaxRetries(), 3);
    assertEquals(config.getSalesforceMaxRecords(), 50000);
  }
}
