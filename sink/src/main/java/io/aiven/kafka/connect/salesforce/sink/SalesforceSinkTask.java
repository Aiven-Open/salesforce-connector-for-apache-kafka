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

import io.aiven.kafka.connect.salesforce.common.VisibleForTesting;
import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.sink.config.SalesforceSinkConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.ErrantRecordReporter;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A task that writes records to Salesforce. */
public final class SalesforceSinkTask extends SinkTask {
  private static final Logger LOG = LoggerFactory.getLogger(SalesforceSinkTask.class);

  private SalesforceSinkConfig config;
  private BulkApiClient api;
  private final List<SinkRecord> buffer = new ArrayList<>();
  private ErrantRecordReporter errantRecordReporter;

  /** Constructor */
  public SalesforceSinkTask() {}

  /** Constructor with an injected Bulk API for testing */
  @VisibleForTesting
  SalesforceSinkTask(BulkApiClient api) {
    this.api = api;
  }

  /** Lazily creates the bulk API client. */
  private BulkApiClient getApi() {
    if (api == null) {
      api = new BulkApiClient(config);
    }
    return api;
  }

  @Override
  public String version() {
    return SalesforceSinkConnector.VERSION;
  }

  @Override
  public void start(final Map<String, String> props) {
    Objects.requireNonNull(props, "props cannot be null");
    config = new SalesforceSinkConfig(props);
    errantRecordReporter = context.errantRecordReporter();
    LOG.info("Start Salesforce sink task ({})", config.getSinkObject());
  }

  @Override
  public void put(final Collection<SinkRecord> records) {
    if (!records.isEmpty()) {
      buffer.addAll(records);
    }
  }

  private void reportUnsupportedValue(SinkRecord record) {
    var msg =
        String.format(
            "Skipping record with unsupported value: {} and schema: {}",
            record.value().getClass(),
            record.valueSchema());
    LOG.error(msg);
    errantRecordReporter.report(record, new Throwable(msg));
  }

  @Override
  public void flush(final Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
    if (buffer.isEmpty()) {
      return;
    }

    LOG.info("Flushing {} sink record(s)", buffer.size());

    // TODO: In configuration, we could specify the exact header set to use instead of dynamically
    // detecting it.

    // In the first pass, discover the column headers (in a TreeMap for consistent, alphabetical
    // column order).
    final Map<String, Integer> columns = new TreeMap<>();
    final List<SinkRecord> valid = new ArrayList<>(buffer.size());
    int skipped = 0;
    // Cache STRUCT schemas for the ideal path where all records have the same schema.
    final Set<Schema> seen = new HashSet<>();

    // Process each record once: extract columns and filter valid records
    for (SinkRecord record : buffer) {
      if (record.value() instanceof Map<?, ?> mapValue) {
        valid.add(record);
        for (Object key : mapValue.keySet()) {
          columns.putIfAbsent(key.toString(), 0);
        }
      } else if (record.valueSchema() != null
          && record.valueSchema().type() == Schema.Type.STRUCT) {
        // Skip processing if we've already seen this schema
        valid.add(record);
        if (seen.contains(record.valueSchema())) continue;
        seen.add(record.valueSchema());
        // Discover any new columns from this STRUCT
        for (Field field : record.valueSchema().fields()) {
          columns.putIfAbsent(field.name(), 0); // Placeholder, indices assigned later
        }
      } else {
        skipped++;
        reportUnsupportedValue(record);
      }
    }

    if (skipped > 0) {
      LOG.warn("Flush encountered {} records with unsupported schemas or values", skipped);
    }

    // There were records, but none that were ingestible. Fail the entire batch.
    if (columns.isEmpty()) {
      buffer.clear();
      throw new ConnectException(
          "Flush didn't encounter any struct or map values; skipping Salesforce bulk insert.");
    }

    // TODO: Do we want to check for invalid columns in the struct?
    // Should we ignore them or fail those records?

    // Assign sequential indices to columns in alphabetical order (TreeMap iteration order)
    int columnIndex = 0;
    for (String columnName : columns.keySet()) {
      columns.put(columnName, columnIndex++);
    }

    final int columnCount = columns.size();

    // Convert valid records to CSV rows using discovered column positions
    Stream<Object[]> dataToSend =
        valid.stream()
            .map(
                record -> {
                  var orderedValues = new Object[columnCount];
                  if (record.value() instanceof Map<?, ?> mapValue) {
                    for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                      orderedValues[columns.get(entry.getKey().toString())] = entry.getValue();
                    }
                  } else if (record.value() instanceof Struct structValue) {
                    for (Field f : record.valueSchema().fields()) {
                      orderedValues[columns.get(f.name())] = structValue.get(f);
                    }
                  } else {
                    // This should never occur because of the first pass
                    reportUnsupportedValue(record);
                  }
                  return orderedValues;
                });

    var response =
        getApi().multipartInsert(config.getSinkObject(), columns.keySet().toArray(), dataToSend);

    if (response.isEmpty()) {
      throw new ConnectException(
          "Salesforce bulk ingest submission failed or returned an empty response");
    }

    LOG.info(
        "Bulk ingest job {} submitted with state {}, waiting for completion",
        response.get().getId(),
        response.get().getState());
    handleFinalJobState(getApi().waitForJob(response.get(), BulkApiClient.URI_INGEST_JOB_INFO));

    buffer.clear();
  }

  private static void handleFinalJobState(QueryResponse info) throws ConnectException {
    switch (info.getState()) {
      case JobComplete:
        LOG.info("Bulk ingest job {} completed successfully", info.getId());
        break;
      case Failed:
        LOG.error("Bulk ingest job {} failed: {}", info.getId(), info.getErrorMessage());
        throw new ConnectException(
            String.format(
                "Salesforce bulk ingest job %s failed: %s", info.getId(), info.getErrorMessage()));
      case Aborted:
        LOG.error("Bulk ingest job {} aborted.", info.getId());
        throw new ConnectException(
            String.format("Salesforce bulk ingest job %s was aborted", info.getId()));
      default:
        // Handle timeout or other unexpected states (InProgress, Submitted, UploadComplete, Open,
        // etc.)
        if (info.getState().isExecuting()) {
          LOG.error(
              "Bulk ingest job {} timed out while still in state: {}",
              info.getId(),
              info.getState());
          throw new ConnectException(
              String.format(
                  "Salesforce bulk ingest job %s timed out while still in state: %s",
                  info.getId(), info.getState()));
        } else {
          LOG.warn(
              "Bulk ingest job {} ended in unexpected state: {}", info.getId(), info.getState());
          throw new ConnectException(
              String.format(
                  "Salesforce bulk ingest job %s ended in unexpected state: %s",
                  info.getId(), info.getState()));
        }
    }
  }

  @Override
  public void stop() {
    buffer.clear();
    api = null;
    LOG.info("Stop Salesforce sink task");
  }
}
