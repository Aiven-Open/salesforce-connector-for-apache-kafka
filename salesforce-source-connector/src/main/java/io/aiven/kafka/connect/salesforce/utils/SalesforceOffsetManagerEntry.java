package io.aiven.kafka.connect.salesforce.utils;

import io.aiven.commons.kafka.connector.source.OffsetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

public class SalesforceOffsetManagerEntry implements OffsetManager.OffsetManagerEntry<SalesforceOffsetManagerEntry>{

  private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceOffsetManagerEntry.class);
  // will be bulk or CDC in the future
  public static final String API_CLIENT = "apiClient";
  // These refer to the Objects in Salesforce e.g. Account, Address, etc
  public static final String OBJECT_NAME = "objectName";
  public static final String LAST_ACTIVITY = "lastActivity";
  public static final String RECORD_COUNT = "recordCount";

  @Override
  public SalesforceOffsetManagerEntry fromProperties(Map<String, Object> map) {
    return null;
  }

  @Override
  public Map<String, Object> getProperties() {
    return Map.of();
  }

  @Override
  public Object getProperty(String s) {
    return null;
  }

  @Override
  public void setProperty(String s, Object o) {

  }

  @Override
  public OffsetManager.OffsetManagerKey getManagerKey() {
    return null;
  }

  @Override
  public void incrementRecordCount() {

  }

  @Override
  public long getRecordCount() {
    return 0;
  }

  @Override
  public int compareTo(SalesforceOffsetManagerEntry o) {
    return 0;
  }
}
