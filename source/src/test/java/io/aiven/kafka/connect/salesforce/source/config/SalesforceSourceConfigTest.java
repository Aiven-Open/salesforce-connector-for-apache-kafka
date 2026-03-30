package io.aiven.kafka.connect.salesforce.source.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.aiven.kafka.connect.salesforce.common.config.SalesforceCommonConfigFragment;
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
    assertEquals("v65.0", config.getSalesforceApiVersion());
    assertEquals(null, config.getSalesforceOauthUri());
    assertEquals(null, config.getLastModifiedStartDateTime());
    assertEquals(Duration.ofSeconds(5), config.getStatusCheckWaitTime());
    assertEquals(Duration.ofSeconds(300), config.getMinimumQueryExecutionDelay());
    assertEquals(3, config.getSalesforceMaxRetries());
    assertEquals(50000, config.getSalesforceMaxRecords());
  }

  @Test
  void updateValues() {
    final var props = new HashMap<String, String>();
    SalesforceSourceConfigFragment.setter(props)
        .bulkApiQueries("SELECT Id,LastModifiedDate FROM Account");
    SalesforceCommonConfigFragment.setter(props)
        .apiVersion("v1.0")
        .maxRecords(20)
        .maxRetries(1)
        .topicPrefix("unit-test")
        .oauthUri("https://oauth.uri")
        .oauthClientSecret("ClientSecret")
        .oauthClientId("ClientId");

    SalesforceSourceConfig config = new SalesforceSourceConfig(props);
    assertEquals("v1.0", config.getSalesforceApiVersion());
    assertEquals(20, config.getSalesforceMaxRecords());
    assertEquals(1, config.getSalesforceMaxRetries());
    assertEquals("unit-test", config.getTopicPrefix());
    assertEquals("https://oauth.uri", config.getSalesforceOauthUri());
    assertEquals("ClientId", config.getOauthClientId());
    assertEquals("ClientSecret", config.getOauthClientSecret());
  }
}
