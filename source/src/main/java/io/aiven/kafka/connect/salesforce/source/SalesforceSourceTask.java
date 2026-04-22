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
package io.aiven.kafka.connect.salesforce.source;

import io.aiven.commons.kafka.connector.source.AbstractSourceTask;
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecord;
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecordIterator;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.kafka.connector.source.config.SourceConfigFragment;
import io.aiven.commons.kafka.connector.source.extractor.CsvExtractor;
import io.aiven.kafka.connect.salesforce.common.VisibleForTesting;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.common.time.InstantUtil;
import io.aiven.kafka.connect.salesforce.source.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.source.model.BulkApiSourceData;
import io.aiven.kafka.connect.salesforce.source.utils.SalesforceOffsetManagerEntry;
import io.aiven.kafka.connect.salesforce.source.utils.Version;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The salesforce source task is called by the kafka connect framework to start the Salesforce
 * source connector. It configures the connector and starts the task.
 */
public final class SalesforceSourceTask extends AbstractSourceTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceSourceTask.class);

  /**
   * The header from the CSV results are case sensitive the LastModifiedDate has a leading
   * capitalized letter
   */
  private static final String LAST_MODIFIED_DATE_CSV_HEADER = "LastModifiedDate";

  /** The offset manager this task uses */
  private OffsetManager offsetManager;

  private Map<String, Instant> lastSeenModifiedDate;

  /** Should check about adding this */
  public SalesforceSourceTask() {
    super();
  }

  /**
   * Called by {@link #start} to allows the concrete implementation to configure itself based on
   * properties.
   *
   * @param props The properties to use for configuration.
   * @param offsetManager the OffsetManager to use.
   * @return A SourceCommonConfig based configuration.
   */
  @Override
  protected SourceCommonConfig configure(Map<String, String> props, OffsetManager offsetManager) {
    LOGGER.info("Salesforce Source task started.");
    this.offsetManager = new OffsetManager(context);
    this.lastSeenModifiedDate = new HashMap<>();
    // set the csv transformer for bulk api
    SourceConfigFragment.setter(props).extractorClass(CsvExtractor.class);
    return new SalesforceSourceConfig(props);
  }

  /**
   * Called by the test suit to test the functionality of the SalesforceSourceTask
   *
   * @param props The properties that are being used in the config for the test
   * @param offsetManager The offset Manager that is being used in the test
   * @param lastSeenModifiedDate The last seen modifiedDate map, contains all the sqlQueryHash to
   *     lastSeenModifiedDates
   * @return The SourceCommonConfig for the test
   */
  @VisibleForTesting
  protected SourceCommonConfig configure(
      Map<String, String> props,
      OffsetManager offsetManager,
      HashMap<String, Instant> lastSeenModifiedDate) {
    LOGGER.info("Salesforce Source task configured for testing.");
    this.offsetManager = offsetManager;
    this.lastSeenModifiedDate = lastSeenModifiedDate;
    // set the csv transformer for bulk api
    SourceConfigFragment.setter(props).extractorClass(CsvExtractor.class);
    return new SalesforceSourceConfig(props);
  }

  /**
   * Gets the iterator of SourceRecords. The iterator that SourceRecords are extracted from for a
   * poll event. When this iterator runs out of records it should attempt to reset and read more
   * records from the backend on the next {@code hasNext()} call. In this way it should detect when
   * new data has been added to the backend and continue processing.
   *
   * <p>This method should handle any backend exception that can be retried. Any runtime exceptions
   * that are thrown when this iterator executes may cause the task to abort.
   *
   * @param config the SourceCommonConfig instance.
   * @return The iterator of SourceRecords.
   */
  @Override
  protected EvolvingSourceRecordIterator getIterator(SourceCommonConfig config) {
    SalesforceSourceConfig myConfig = (SalesforceSourceConfig) config;
    return new EvolvingSourceRecordIterator(
        myConfig, new BulkApiSourceData(myConfig, offsetManager, lastSeenModifiedDate));
  }

  @Override
  protected void closeResources() {
    // no resources to close
  }

  @Override
  public String version() {
    return Version.VERSION;
  }

  @Override
  public void commit() {
    LOGGER.info("Committed all records through last poll()");
  }

  @Override
  protected EvolvingSourceRecord lastEvolution(EvolvingSourceRecord evolvingSourceRecord) {
    try {
      String value = getLastModifiedDateFromRecord(evolvingSourceRecord);
      BulkApiKey key = (BulkApiKey) evolvingSourceRecord.getNativeKey();
      Instant lastModifiedDate = lastSeenModifiedDate.getOrDefault(key.getQueryHash(), null);
      if (lastModifiedDate != null) {
        lastSeenModifiedDate.put(
            key.getQueryHash(), InstantUtil.getLatest(value, lastModifiedDate));
      } else {
        lastSeenModifiedDate.put(key.getQueryHash(), InstantUtil.parseString(value));
      }
      // Update to the last seen timestamp so we
      // know where to begin from on a restart
      SalesforceOffsetManagerEntry offsetRecord =
          (SalesforceOffsetManagerEntry) evolvingSourceRecord.getOffsetManagerEntry();
      offsetRecord.setLastModified(lastSeenModifiedDate.get(key.getQueryHash()));
      evolvingSourceRecord.setOffsetManagerEntry(offsetRecord);
    } catch (Exception e) {
      // nothing
      LOGGER.error("Exception caught updating the LastModifiedDate in lastEvolution. ", e);
    }
    return evolvingSourceRecord;
  }

  private static String getLastModifiedDateFromRecord(EvolvingSourceRecord evolvingSourceRecord) {
    if (evolvingSourceRecord.getValue().value() instanceof Struct) {
      Struct struct = (Struct) evolvingSourceRecord.getValue().value();
      return struct.getString(LAST_MODIFIED_DATE_CSV_HEADER);
    } else {
      LinkedHashMap<String, String> value = (LinkedHashMap) evolvingSourceRecord.getValue().value();
      return value.get(LAST_MODIFIED_DATE_CSV_HEADER);
    }
  }
}
