/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aiven.kafka.connect.salesforce.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.aiven.commons.kafka.connector.source.EvolvingSourceRecord;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SalesforceSourceTaskTest {

  private static final String QUERY = "SELECT Id,LastModifiedDAte From Account";
  private static final String BULK_API = "bulkApi";
  private SalesforceSourceTask task;
  private OffsetManager offsetManager;
  private HashMap<String, Instant> map;

  @BeforeEach
  void setup() {
    offsetManager = Mockito.mock(OffsetManager.class);
    task = Mockito.mock(SalesforceSourceTask.class);
    when(task.lastEvolution(any())).thenCallRealMethod();
    when(task.configure(any(), any(), any())).thenCallRealMethod();
    map = new HashMap<>();
    task.configure(new HashMap<>(), offsetManager, map);
  }

  @Test
  void lastModDateIsUpdatedFromNull() {

    Instant lastModDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    BulkApiKey apiKey = new BulkApiKey(BULK_API, QUERY, lastModDate.toString(), "");
    task.lastEvolution(mockEvolvingSourceRecord(apiKey, lastModDate.toString()));
    assertEquals(map.get(apiKey.getQueryHash()), lastModDate);
  }

  @Test
  void lastModDateIsNotUpdatedFromOlderTimestamp() {
    Instant lastModDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    BulkApiKey apiKey = new BulkApiKey(BULK_API, QUERY, lastModDate.toString(), "");
    task.lastEvolution(mockEvolvingSourceRecord(apiKey, lastModDate.toString()));
    assertEquals(map.get(apiKey.getQueryHash()), lastModDate);
    Instant olderLastModDate = lastModDate.minusSeconds(5);

    apiKey = new BulkApiKey(BULK_API, QUERY, olderLastModDate.toString(), "");
    task.lastEvolution(mockEvolvingSourceRecord(apiKey, olderLastModDate.toString()));
    // LastSeenModDate should still be the same as it is newer.
    assertEquals(map.get(apiKey.getQueryHash()), lastModDate);
  }

  @Test
  void lastModDateIsUpdatedToNewerTimestamp() {
    Instant lastModDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    BulkApiKey apiKey = new BulkApiKey(BULK_API, QUERY, lastModDate.toString(), "");
    task.lastEvolution(mockEvolvingSourceRecord(apiKey, lastModDate.toString()));
    assertEquals(map.get(apiKey.getQueryHash()), lastModDate);
    Instant newerLastModDate = lastModDate.plusSeconds(1);

    apiKey = new BulkApiKey(BULK_API, QUERY, newerLastModDate.toString(), "");
    task.lastEvolution(mockEvolvingSourceRecord(apiKey, newerLastModDate.toString()));
    // LastSeenModDate should updated as its newer
    assertEquals(map.get(apiKey.getQueryHash()), newerLastModDate);
  }

  private EvolvingSourceRecord mockEvolvingSourceRecord(
      BulkApiKey apiKey, String lastModifiedDate) {
    EvolvingSourceRecord record = Mockito.mock(EvolvingSourceRecord.class);
    when(record.getValue())
        .thenReturn(createSchemaAndValueData("RandomIdThatIsNotUsed", lastModifiedDate));
    when(record.getNativeKey()).thenReturn(apiKey);
    return record;
  }

  private SchemaAndValue createSchemaAndValueData(String id, String lastModifiedDate) {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("Id", id);
    map.put("LastModifiedDate", lastModifiedDate);
    return new SchemaAndValue(null, map);
  }
}
