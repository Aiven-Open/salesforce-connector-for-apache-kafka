package io.aiven.kafka.connect.salesforce;

import io.aiven.commons.kafka.connector.source.AbstractSourceTask;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.Iterator;
import java.util.Map;

public class SalesforceSourceTask extends AbstractSourceTask {
  @Override
  protected Iterator<SourceRecord> getIterator(
      io.aiven.commons.timing.BackoffConfig backoffConfig) {
    return null;
  }

  @Override
  protected SourceCommonConfig configure(Map<String, String> map) {
    return null;
  }

  @Override
  protected void closeResources() {

  }

  @Override
  public String version() {
    return "";
  }
}
