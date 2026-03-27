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
package io.aiven.kafka.connect.salesforce.source.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.aiven.commons.kafka.connector.source.EvolvingSourceRecord;
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecordIterator;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceConfigFragment;
import io.aiven.commons.kafka.connector.source.extractor.CsvExtractor;
import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import io.aiven.kafka.connect.salesforce.source.BulkApiQueryEngine;
import io.aiven.kafka.connect.salesforce.source.config.SalesforceSourceConfig;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class BulkApiSourceDataTest {

  public static final String MSG_HEADER =
      CSVFormat.RFC4180.format("Id", "LastModifiedDate", "Name");
  private BulkApiSourceData sourceData;
  private OffsetManager offsetManager;
  private BulkApiQueryEngine engine;
  private SalesforceSourceConfig config;
  private BulkApiClient apiClient;

  @BeforeEach
  void setup() {
    offsetManager = Mockito.mock(OffsetManager.class);
    config = Mockito.mock(SalesforceSourceConfig.class);
    apiClient = Mockito.mock(BulkApiClient.class);
  }

  @Test
  void evolvingSourceRecordIterator() throws InterruptedException {
    Map<String, String> props = getConfig();
    SourceConfigFragment.setter(props).extractorClass(CsvExtractor.class);
    SalesforceSourceConfig myConfig = (SalesforceSourceConfig) new SalesforceSourceConfig(props);
    EvolvingSourceRecordIterator sourceRecordIterator =
        new EvolvingSourceRecordIterator(
            myConfig, new BulkApiSourceData(myConfig, offsetManager, engine, new HashMap<>()));

    engine = new BulkApiQueryEngine(config,apiClient);
    String jobId = "jobId";
    when(apiClient.submitQueryJob(eq(String.class))).thenReturn(jobId);
    QueryResponse queryResponse = new QueryResponse();
    queryResponse.setState(JobState.JobComplete);
    queryResponse.setId(jobId);
    queryResponse.setObject("Account");
    queryResponse.setCreatedDate(Instant.now().toString());
    when(apiClient.queryJobStatus(eq(jobId))).thenReturn(Optional.of(queryResponse));
    when(apiClient.getJobResults(eq(jobId), eq(null), eq())).thenReturn(generateCsvRecords(0,987, 3,times()));

    

    int numberOfRecordsProcessed = 0;
    List<EvolvingSourceRecord> sourceRecord = new ArrayList<>();
    while (sourceRecordIterator.hasNext()) {
      EvolvingSourceRecord next = sourceRecordIterator.next();
      sourceRecord.add(next);
      var naveInfo = next.getSourceNativeInfo();
      OffsetManager.OffsetManagerEntry entry = next.getOffsetManagerEntry();
      int recordCount = (int) entry.getRecordCount();
      int totalCount = (int) entry.getProperty("totalRecordCount");
      // where number of records minus recordCount handles skips
      numberOfRecordsProcessed = totalCount;
      System.out.println(naveInfo);
    }
    assertFalse(sourceRecordIterator.hasNext());
  }

//  @Test
//  void bulkApiSourceDataStream() {
//    Map<String, String> props = getConfig();
//    SourceConfigFragment.setter(props).extractorClass(CsvExtractor.class);
//    SalesforceSourceConfig myConfig = (SalesforceSourceConfig) new SalesforceSourceConfig(props);
//    BulkApiSourceData bulkApiSourceData =
//        new BulkApiSourceData(myConfig, offsetManager, new HashMap<>());
//    AtomicInteger numberOfRecordsProcessed = new AtomicInteger();
//    Iterator<BulkApiNativeInfo> iter = bulkApiSourceData.getSalesforceBulkApiStream().iterator();
//    while (iter.hasNext()) {
//      BulkApiNativeInfo attempt = iter.next();
//      System.out.println(attempt);
//    }
//
//    int i = numberOfRecordsProcessed.get();
//    System.out.println(i);
//  }

  /**
   * Creates Csv test data.
   *
   * @param recordCount the number of records to create.
   * @param lastModifiedDateTimes the list of lastModifiedDate's that are to be returned for the csv
   *     entries.
   * @return the string representing the csv records.
   */
  private static String generateCsvRecords(
      final int skipRecords,
      final int messageId,
      final int recordCount,
      final List<String> lastModifiedDateTimes) {
    final StringBuilder csvRecords = new StringBuilder(MSG_HEADER).append(System.lineSeparator());
    for (int i = skipRecords; i < (skipRecords + recordCount); i++) {
      csvRecords
          .append(generateCsvRecord(messageId + i, lastModifiedDateTimes.get(i + skipRecords)))
          .append("\n");
    }
    return csvRecords.toString();
  }

  /**
   * Generates a single JSON record
   *
   * @param messageId the id for the record
   * @param msg the message for the record
   * @return a standard JSON test record.
   */
  private static String generateCsvRecord(final int messageId, final String msg) {
    return CSVFormat.RFC4180.format(messageId, msg, messageId);
  }

  private List<String> times() {
    return List.of(
        "2025-11-08T00:00:00Z",
        "2025-11-08T15:00:00Z",
        "2025-11-13T05:00:13Z",
        "2025-12-13T14:20:00Z",
        "2026-01-01T07:22:00Z",
        "2025-01-01T13:00:00Z",
        "2026-01-01T15:24:00Z",
        "2026-01-01T00:00:07Z",
        "2026-01-01T00:02:00Z",
        "2026-01-11T10:00:00Z",
        "2026-01-12T04:00:00Z",
        "2026-01-12T13:00:00Z",
        "2026-01-12T14:00:00Z",
        "2026-01-12T15:00:00Z",
        "2026-01-13T13:00:00Z",
        "2026-01-14T12:00:00Z",
        "2026-01-15T08:00:00Z",
        "2026-01-16T13:00:00Z",
        "2026-02-07T22:00:00Z",
        "2026-02-08T23:00:00Z",
        "2026-03-08T19:00:00Z",
        "2026-03-18T18:00:00Z");
  }

  private Map<String, String> getConfig() {
    HashMap<String, String> map = new HashMap<>();
    map.put(
        "connector.class", "io.aiven.kafka.connect.salesforce.source.SalesforceSourceConnector");
    map.put("tasks.max", "1");
    map.put("topics.prefix", "salesforce.test");
    map.put("max.retries", "3");
    map.put("salesforce.lastModifiedStartDate", "2025-11-08T00:00:00Z");
    map.put("salesforce.api.version", "v65.0");
    map.put("salesforce.soql.query.wait", "30");
    map.put("salesforce.max.records", "5");
    map.put("salesforce.client.secret", "Client.Secret");
    map.put("salesforce.client.id", "Client.Id");
    map.put("salesforce.uri", "http://localhost");
    map.put("salesforce.oauth.uri", "https://localhost/services/oauth2/token");
    map.put("topic.prefix", "salesforce.bulk");
    map.put("salesforce.bulk.api.queries", "SELECT Id, LastModifiedDate, Name FROM Account ");
    return map;
  }
}
