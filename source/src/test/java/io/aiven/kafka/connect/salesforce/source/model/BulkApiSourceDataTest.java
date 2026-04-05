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

import static io.aiven.kafka.connect.salesforce.source.utils.SalesforceOffsetManagerEntry.LAST_MODIFIED_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecord;
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecordIterator;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceConfigFragment;
import io.aiven.commons.kafka.connector.source.extractor.CsvExtractor;
import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.common.bulk.model.SalesforceContext;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResult;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResultResponse;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.time.InstantUtil;
import io.aiven.kafka.connect.salesforce.source.BulkApiQueryEngine;
import io.aiven.kafka.connect.salesforce.source.config.SalesforceSourceConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * This test class checks if the iteration of the source records retrieves all the expected records.
 */
public class BulkApiSourceDataTest {

  public static final String MSG_HEADER =
      CSVFormat.RFC4180.format("Id", "LastModifiedDate", "Name");
  public static final String SOQL_QUERY = "SELECT Id, LastModifiedDate, Name FROM Account ";
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

  @ParameterizedTest
  @MethodSource("testData")
  void evolvingSourceRecordIteratorShouldProcessAllRecords(
      int expectedRecords, int resultsPerPage, int expectedPaginatedPages) {
    SalesforceSourceConfig myConfig =
        setUpSalesforceBulkApiClientMock(expectedRecords, resultsPerPage);
    EvolvingSourceRecordIterator evolvingSourceRecordIterator =
        new EvolvingSourceRecordIterator(
            myConfig, new BulkApiSourceData(myConfig, offsetManager, engine, new HashMap<>()));

    List<EvolvingSourceRecord> sourceRecords = new ArrayList<>();

    while (evolvingSourceRecordIterator.hasNext()) {
      EvolvingSourceRecord evolvingSourceRecord = evolvingSourceRecordIterator.next();
      sourceRecords.add(evolvingSourceRecord);
    }
    assertEquals(expectedRecords, sourceRecords.size());
    assertFalse(evolvingSourceRecordIterator.hasNext());
    verify(apiClient, times(1)).submitQueryJob(anyString());
    verify(apiClient, times(1))
        .getJobResults(anyString(), eq(null), anyString(), any(BulkApiKey.class));
    verify(apiClient, times(expectedPaginatedPages))
        .getJobResults(anyString(), anyString(), anyString(), any(BulkApiKey.class));
  }

  @ParameterizedTest
  @MethodSource("testData")
  void BulkApiSourceDataStream(
      int expectedRecords, int resultsPerPage, int expectedPaginatedPages) {

    SalesforceSourceConfig myConfig =
        setUpSalesforceBulkApiClientMock(expectedRecords, resultsPerPage);
    BulkApiSourceData bulkApiSourceData =
        new BulkApiSourceData(myConfig, offsetManager, engine, new HashMap<>());

    List<EvolvingSourceRecord> sourceRecord = new ArrayList<>();
    // Start Page Count at 1
    AtomicInteger recordCount = new AtomicInteger();
    Iterator<BulkApiNativeInfo> salesforceBulkApiIterator =
        bulkApiSourceData.getSalesforceBulkApiIterator();
    salesforceBulkApiIterator.forEachRemaining(
        entry -> recordCount.addAndGet(((SalesforceContext) entry.getContext()).getTotalRecords()));
    // Check if it has the expected number of pages
    assertEquals(expectedRecords, recordCount.get());
    verify(apiClient, times(1)).submitQueryJob(anyString());
    verify(apiClient, times(1))
        .getJobResults(anyString(), eq(null), anyString(), any(BulkApiKey.class));
    verify(apiClient, times(expectedPaginatedPages))
        .getJobResults(anyString(), anyString(), anyString(), any(BulkApiKey.class));
  }

  @Test
  void evolvingSourceRecordIteratorNoRecordsReturned() {
    SalesforceSourceConfig myConfig = setUpSalesforceBulkApiClientMock(0, 500);
    // Return no records
    when(apiClient.getJobResults(eq("jobId"), eq(null), eq("Account"), any()))
        .thenReturn(getResults(1, 0, 0, 500));
    EvolvingSourceRecordIterator evolvingSourceRecordIterator =
        new EvolvingSourceRecordIterator(
            myConfig, new BulkApiSourceData(myConfig, offsetManager, engine, new HashMap<>()));

    List<EvolvingSourceRecord> sourceRecords = new ArrayList<>();

    while (evolvingSourceRecordIterator.hasNext()) {
      EvolvingSourceRecord evolvingSourceRecord = evolvingSourceRecordIterator.next();
      sourceRecords.add(evolvingSourceRecord);
    }
    assertEquals(0, sourceRecords.size());
    assertFalse(evolvingSourceRecordIterator.hasNext());
    verify(apiClient, times(1)).submitQueryJob(anyString());
    verify(apiClient, times(1))
        .getJobResults(anyString(), eq(null), anyString(), any(BulkApiKey.class));
    verify(apiClient, times(0))
        .getJobResults(anyString(), anyString(), anyString(), any(BulkApiKey.class));
  }

  @Test
  void BulkApiSourceDataStreamNoRecordsReturned() {

    SalesforceSourceConfig myConfig = setUpSalesforceBulkApiClientMock(0, 500);
    // Return no records
    when(apiClient.getJobResults(eq("jobId"), eq(null), eq("Account"), any()))
        .thenReturn(getResults(1, 0, 0, 500));
    BulkApiSourceData bulkApiSourceData =
        new BulkApiSourceData(myConfig, offsetManager, engine, new HashMap<>());

    // Start Page Count at 1
    AtomicInteger recordCount = new AtomicInteger();
    Iterator<BulkApiNativeInfo> salesforceBulkApiIterator =
        bulkApiSourceData.getSalesforceBulkApiIterator();
    salesforceBulkApiIterator.forEachRemaining(
        entry -> recordCount.addAndGet(((SalesforceContext) entry.getContext()).getTotalRecords()));
    // Check if it has the expected number of pages
    assertEquals(0, recordCount.get());
    verify(apiClient, times(1)).submitQueryJob(anyString());
    verify(apiClient, times(1))
        .getJobResults(anyString(), eq(null), anyString(), any(BulkApiKey.class));
    verify(apiClient, times(0))
        .getJobResults(anyString(), anyString(), anyString(), any(BulkApiKey.class));
  }

  @Test
  void bulkApiRecoverFromRestart() {

    SalesforceSourceConfig myConfig = setUpSalesforceBulkApiClientMock(0, 500);
    // Return no records
    when(apiClient.getJobResults(eq("jobId"), eq(null), eq("Account"), any()))
        .thenReturn(getResults(1, 0, 0, 500));
    String lastModifiedDate = "2026-01-13T13:00:00.000Z";
    Long lastModDateMillis = InstantUtil.parseString(lastModifiedDate).toEpochMilli();
    when(offsetManager.getEntryData(any()))
        .thenReturn(Optional.of(Map.of(LAST_MODIFIED_DATE, lastModDateMillis)));
    BulkApiSourceData bulkApiSourceData =
        new BulkApiSourceData(myConfig, offsetManager, engine, new HashMap<>());

    // Start Page Count at 1
    AtomicInteger recordCount = new AtomicInteger();
    Iterator<BulkApiNativeInfo> salesforceBulkApiIterator =
        bulkApiSourceData.getSalesforceBulkApiIterator();
    salesforceBulkApiIterator.forEachRemaining(
        entry -> recordCount.addAndGet(((SalesforceContext) entry.getContext()).getTotalRecords()));
    // Check if it has the expected number of pages
    assertEquals(0, recordCount.get());
    verify(apiClient, times(1)).submitQueryJob(getSOQLQuery(lastModifiedDate, true));
    verify(apiClient, times(1))
        .getJobResults(anyString(), eq(null), anyString(), any(BulkApiKey.class));
    verify(apiClient, times(0))
        .getJobResults(anyString(), anyString(), anyString(), any(BulkApiKey.class));
  }

  private static @Nullable String getNextLocator(int pageNumber) {
    return pageNumber != 1 ? String.valueOf(pageNumber - 1) : null;
  }

  private CompletableFuture<BulkApiResultResponse> getResults(
      final int page, final int numberOfRecords, final int recordCount, final int resultsPerPage) {
    BulkApiResultResponse response = new BulkApiResultResponse();
    response.setLocator(
        hasNextPage(page, recordCount, resultsPerPage) ? String.valueOf(page) : null);
    response.setApiUsage("15/1000");
    response.setNumberOfRecords(numberOfRecords);
    BulkApiResult result =
        new BulkApiResult(
            new NativeInfo<>(
                new BulkApiKey(
                    "bulkApi", "SOQLQuery", Instant.now().toString(), response.getLocator()),
                generateCsvRecords(page, numberOfRecords, lastModifiedDates())),
            "Account");
    response.setResult(result);
    return CompletableFuture.completedFuture(response);
  }

  private static boolean hasNextPage(int page, int recordCount, int resultsPerPage) {
    return recordCount - (resultsPerPage * page) > 0;
  }

  /**
   * Creates Csv test data.
   *
   * @param recordCount the number of records to create.
   * @param lastModifiedDateTimes the list of lastModifiedDate's that are to be returned for the csv
   *     entries.
   * @return the string representing the csv records.
   */
  private static String generateCsvRecords(
      final int messageId, final int recordCount, final List<String> lastModifiedDateTimes) {
    final StringBuilder csvRecords = new StringBuilder(MSG_HEADER).append(System.lineSeparator());
    for (int i = 0; i < recordCount; i++) {
      csvRecords
          .append(generateCsvRecord(messageId + i, lastModifiedDateTimes.get(i)))
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

  private String getSOQLQuery(String lastModifiedDate, boolean isRecovery) {
    return String.format(
        SOQL_QUERY + "WHERE  LastModifiedDate %s %s ORDER BY LastModifiedDate ASC",
        isRecovery ? ">=" : ">",
        lastModifiedDate);
  }

  private List<String> lastModifiedDates() {
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
    map.put("salesforce.bulk.api.queries", SOQL_QUERY);
    return map;
  }

  private SalesforceSourceConfig setUpSalesforceBulkApiClientMock(
      int expectedRecords, int resultsPerPage) {
    int pageNumber = 1;
    Map<String, String> props = getConfig();
    SourceConfigFragment.setter(props).extractorClass(CsvExtractor.class);

    engine = new BulkApiQueryEngine(config, apiClient);

    String jobId = "jobId";
    when(apiClient.submitQueryJob(anyString())).thenReturn(Optional.of(jobId));
    QueryResponse queryResponse = new QueryResponse();
    queryResponse.setState(JobState.JobComplete);
    queryResponse.setId(jobId);
    queryResponse.setObject("Account");
    queryResponse.setCreatedDate(Instant.now().toString());
    when(apiClient.queryJobStatus(eq(jobId))).thenReturn(Optional.of(queryResponse));
    for (int i = 0; i < expectedRecords; i = i + resultsPerPage) {
      CompletableFuture<BulkApiResultResponse> results =
          getResults(
              pageNumber,
              Math.min(resultsPerPage, expectedRecords - i),
              expectedRecords,
              resultsPerPage);
      when(apiClient.getJobResults(eq(jobId), eq(getNextLocator(pageNumber)), eq("Account"), any()))
          .thenReturn(results);
      pageNumber++;
    }
    return new SalesforceSourceConfig(props);
  }

  /**
   * Returns in order the numberOfRecords to be produced The number of entries per page there should
   * be The number of paginated pages that should be produced
   *
   * @return The test data for the unit tests
   */
  private static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of("22", "3", 7),
        Arguments.of("22", "7", 3),
        Arguments.of("22", "21", 1),
        Arguments.of("22", "22", 0));
  }
}
