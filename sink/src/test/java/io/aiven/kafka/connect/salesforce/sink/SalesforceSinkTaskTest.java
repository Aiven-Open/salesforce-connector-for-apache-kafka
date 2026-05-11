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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.exceptions.SFAuthException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTaskContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link SalesforceSinkTask}. */
public final class SalesforceSinkTaskTest {

  /**
   * Creates a test configuration for the Salesforce sink connector to write to Salesforce Account
   * objects.
   *
   * <p>Uses fake credentials by default, but can be overridden with environment variables for
   * integration testing:
   *
   * <ul>
   *   <li>{@code SFTEST_CONSUMER_KEY} - OAuth client ID
   *   <li>{@code SFTEST_CONSUMER_SECRET} - OAuth client secret
   *   <li>{@code SFTEST_INSTANCE_URL} - Salesforce instance URL
   * </ul>
   *
   * @return a map containing the connector configuration properties
   */
  public static Map<String, String> createTestConfig() {
    var clientId = Optional.ofNullable(System.getenv("SFTEST_CONSUMER_KEY")).orElse("<CLIENT_ID>");
    var clientSecret =
        Optional.ofNullable(System.getenv("SFTEST_CONSUMER_SECRET")).orElse("<CLIENT_SECRET>");
    var uri =
        Optional.ofNullable(System.getenv("SFTEST_INSTANCE_URL")).orElse("https://example.com/");

    return Map.of(
        "salesforce.bulk.api.sink.object",
        "Account",
        "salesforce.client.id",
        clientId,
        "salesforce.client.secret",
        clientSecret,
        "salesforce.oauth.uri",
        uri + "/services/oauth2/token",
        "salesforce.uri",
        uri,
        "offset.flush.interval.ms",
        "60000");
  }

  /**
   * Creates a mocked Bulk API client that simulates Salesforce API behaviour.
   *
   * <p>The mock authenticates successfully, accepts multipart insert requests, and returns a job
   * response with the specified state. The data stream passed to {@code multipartInsert} is
   * consumed and captured into the provided list for test verification.
   *
   * @param multipartIngestState the initial state of the job returned by the mock
   * @param multipartDataCapture a list that will be populated with the data arrays sent to {@link
   *     BulkApiClient#multipartInsert}
   * @return a mocked {@link BulkApiClient} configured for testing
   */
  public static BulkApiClient createApiMock(
      JobState multipartIngestState, List<Object[]> multipartDataCapture) {
    var api = Mockito.mock(BulkApiClient.class);
    try {
      doNothing().when(api).authenticate();
      var qr = new QueryResponse();
      qr.setId("<JOB_ID>");
      qr.setState(multipartIngestState);
      if (multipartIngestState == JobState.Failed) qr.setErrorMessage("<ERROR_MSG>");
      when(api.multipartInsert(any(), any(), any()))
          .thenAnswer(
              invocation -> {
                // We need to capture the stream here or it gets consumed by Mockito
                Stream<Object[]> dataStream = invocation.getArgument(2);
                dataStream.forEach(multipartDataCapture::add);
                return Optional.of(qr);
              });
      when(api.waitForJob(any(), anyString())).thenReturn(qr);
    } catch (SFAuthException ignored) {
      // This shouldn't occur while setting up the mock
    }
    return api;
  }

  @Test
  void testVersion() {
    assertThat(SalesforceSinkConnector.VERSION).isNotEqualTo("unknown");
  }

  /** Tests the successful path of running a task to insert a series of well-formed records. */
  @Test
  void testSuccessfulRecords() {
    var capturedData = new ArrayList<Object[]>();
    var api = createApiMock(JobState.JobComplete, capturedData);

    var task = new SalesforceSinkTask(api);
    task.initialize(Mockito.mock(SinkTaskContext.class));
    task.start(createTestConfig());

    // A good example STRUCT record
    task.put(List.of(createStructRecord("AccountNumber", "1", "Name", "Test1")));

    // Other STRUCT records with fewer fields, different field types, and differently ordered fields
    task.put(
        List.of(
            createStructRecord("AccountNumber", 2, "Name", "Test2"),
            createStructRecord("Name", "Test3", "AccountNumber", "3")));
    task.put(
        List.of(
            createStructRecord("AccountNumber", "4", "Name", "Test\"4", "NumberofLocations__c", 4),
            createStructRecord("Name", "Test5, Inc")));

    // Other MAP records, both with and without schemas
    task.put(
        List.of(
            createMapRecord(
                Schema.STRING_SCHEMA,
                "AccountNumber",
                "6",
                "Name",
                "Test6",
                "NumberofLocations__c",
                "6"),
            createMapRecord(
                Schema.STRING_SCHEMA,
                "Name",
                "Test7",
                "NumberofLocations__c",
                "7",
                "Rating",
                "Hot"),
            createMapRecord(null, "Name", "Test8", "AccountNumber", "8", "NumberofLocations__c", 8),
            createMapRecord(null, "Name", "Test9, Inc")));

    task.flush(Map.of());
    task.stop();

    verify(api, times(1))
        .multipartInsert(
            eq("Account"),
            eq(new Object[] {"AccountNumber", "Name", "NumberofLocations__c", "Rating"}),
            any());
    verify(api, times(1)).waitForJob(any(), anyString());

    assertThat(capturedData)
        .containsExactly(
            new Object[] {"1", "Test1", null, null},
            new Object[] {2, "Test2", null, null},
            new Object[] {"3", "Test3", null, null},
            new Object[] {"4", "Test\"4", 4, null},
            new Object[] {null, "Test5, Inc", null, null},
            new Object[] {"6", "Test6", "6", null},
            new Object[] {null, "Test7", "7", "Hot"},
            new Object[] {"8", "Test8", 8, null},
            new Object[] {null, "Test9, Inc", null, null});
  }

  /** Tests the failure path where one bad column spoils the entire batch. */
  @Test
  void testInvalidFieldInRecords() {
    // TODO: make this mock closer to the actual behaviour
    var api = createApiMock(JobState.Failed, new ArrayList<>());

    var task = new SalesforceSinkTask(api);
    task.initialize(Mockito.mock(SinkTaskContext.class));
    task.start(createTestConfig());
    // An OK record
    task.put(List.of(createStructRecord("AccountNumber", "1", "Name", "Test1", "Rating", "Hot")));
    // A bad record fails the batch because of the invalid column
    task.put(List.of(createStructRecord("Name", "Test2", "Invalid", "2")));
    assertThatThrownBy(() -> task.flush(Map.of()))
        .isInstanceOf(ConnectException.class)
        .hasMessage("Salesforce bulk ingest job <JOB_ID> failed: <ERROR_MSG>");
    task.stop();
  }

  /** Tests the failure path where one record fails out of the batch batch. */
  @Test
  void testInvalidValuesInRecords() {
    // TODO: make this mock closer to the actual behaviour
    var api = createApiMock(JobState.Failed, new ArrayList<>());
    var task = new SalesforceSinkTask(api);

    task.initialize(Mockito.mock(SinkTaskContext.class));
    task.start(createTestConfig());
    task.put(List.of(createStructRecord("Name", "Test1", "NumberofLocations__c", "1")));
    task.put(
        List.of(
            createStructRecord("Name", "Test2", "NumberofLocations__c", "Two"))); // not a number
    assertThatThrownBy(() -> task.flush(Map.of()))
        .isInstanceOf(ConnectException.class)
        .hasMessage("Salesforce bulk ingest job <JOB_ID> failed: <ERROR_MSG>");
    task.stop();
  }

  /** Tests the timeout scenario where a job doesn't complete within the timeout period. */
  @Test
  void testJobTimeout() {
    var api = createApiMock(JobState.InProgress, new ArrayList<>());
    var task = new SalesforceSinkTask(api);

    task.initialize(Mockito.mock(SinkTaskContext.class));
    task.start(createTestConfig());
    task.put(List.of(createStructRecord("Name", "Test1")));
    assertThatThrownBy(() -> task.flush(Map.of()))
        .isInstanceOf(ConnectException.class)
        .hasMessage(
            "Salesforce bulk ingest job <JOB_ID> timed out while still in state: InProgress");
    task.stop();
  }

  /**
   * Build a test {@link SinkRecord} from a list of field names and values, inferring the schema
   * from the values.
   *
   * @param kvs A list of alternating keys and values (e.g. "a", 1, "b", 2).
   * @return A structure created from the keys and values, where the values are either int32,
   *     string, or another struct.
   */
  private static SinkRecord createStructRecord(final Object... kvs) {
    var value = createStruct(kvs);
    return new SinkRecord("topic", 0, null, null, value.schema(), value, 0);
  }

  /**
   * Build a test {@link SinkRecord} with a Map value and potentially a MAP schema
   *
   * @param mapValueSchema If null, create a schemaless record. Otherwise, use this as the MAP value
   *     schema.
   * @param kvs A list of alternating keys and values (e.g. "a", 1, "b", 2).
   * @return A MAP record with string keys and string values.
   */
  private static SinkRecord createMapRecord(Schema mapValueSchema, final Object... kvs) {
    var map = new java.util.HashMap<String, Object>();
    for (int i = 0; (i + 1) < kvs.length; i += 2) {
      map.put(kvs[i].toString(), kvs[i + 1] == null ? null : kvs[i + 1]);
    }
    var schema =
        mapValueSchema != null
            ? SchemaBuilder.map(Schema.STRING_SCHEMA, mapValueSchema).build()
            : null;
    return new SinkRecord("topic", 0, null, null, schema, map, 0);
  }

  /**
   * Build a test {@link Struct} from a list of field names and values, inferring the schema from
   * the values.
   *
   * @param kvs A list of alternating keys and values (e.g. "a", 1, "b", 2).
   * @return A structure created from the keys and values, where the values are either int32,
   *     string, or another struct.
   */
  private static Struct createStruct(final Object... kvs) {
    SchemaBuilder sb = SchemaBuilder.struct();
    for (int i = 0; (i + 1) < kvs.length; i += 2) {
      Schema fieldSchema = Schema.STRING_SCHEMA;
      if (kvs[i + 1] instanceof Integer) {
        fieldSchema = Schema.INT32_SCHEMA;
      } else if (kvs[i + 1] instanceof Struct) {
        fieldSchema = ((Struct) kvs[i + 1]).schema();
      }
      sb = sb.field(kvs[i].toString(), fieldSchema);
    }

    final Schema schema = sb.build();
    final Struct struct = new Struct(schema);
    for (int i = 0; (i + 1) < kvs.length; i += 2) {
      struct.put(kvs[i].toString(), kvs[i + 1]);
    }

    return struct;
  }
}
