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

import static java.util.stream.Collectors.toSet;

import io.aiven.kafka.connect.salesforce.common.VisibleForTesting;
import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.sink.config.SalesforceSinkConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.connector.ConnectRecord;
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

  @Override
  public void flush(final Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
    if (buffer.isEmpty()) {
      return;
    }

    LOG.info("Flushing {} sink record(s)", buffer.size());

    // TODO: In configuration, we could specify the exact header set to use instead of dynamically
    // detecting it.

    // The columns to use for the insert come from the STRUCT schema in the values. Do the first
    // pass to detect the unique set of schemas in play.
    final Set<Schema> vSchemas = buffer.stream().map(ConnectRecord::valueSchema).collect(toSet());

    // Create a tree map with every dynamically discovered column name. Alphabetical ordering is
    // not required but can be useful to have a consistent column order.
    final int[] skippedSchemas = {0};
    final Map<String, Integer> columns =
        vSchemas.stream()
            .flatMap(
                schema -> {
                  if (schema.type() == Schema.Type.STRUCT) {
                    return schema.fields().stream();
                  } else {
                    // Some messages will be unsendable. Warnings are handled later.
                    skippedSchemas[0]++;
                    return Stream.empty();
                  }
                })
            .map(Field::name)
            .unordered()
            .distinct()
            .collect(
                Collectors.toMap(
                    Function.identity(), kv -> 0, (first, ignored) -> first, TreeMap::new));

    if (skippedSchemas[0] > 0) {
      LOG.warn("Flush encountered {} schema(s) without a struct value", skippedSchemas[0]);
    }

    // There were records, but none that were ingestible. Fail the entire batch.
    if (columns.isEmpty()) {
      buffer.clear();
      throw new ConnectException(
          "Flush didn't encounter any struct values; skipping Salesforce bulk insert.");
    }

    // TODO: Do we want to check for invalid columns in the struct?
    // Should we ignore them or fail those records?

    // Give every column a unique position in the output CSV file.
    int i = 0;
    for (String column : columns.keySet()) {
      columns.put(column, i++);
    }

    // In a second pass, convert each record in the buffer into its CSV string
    Stream<Object[]> dataToSend =
        buffer.stream()
            .filter(
                record -> {
                  if (record.valueSchema().type() != Schema.Type.STRUCT) {
                    LOG.error(
                        "Skipping record with non-struct value schema: {}", record.valueSchema());
                    this.errantRecordReporter.report(
                        record, new Throwable("Salesforce only accept records of type STRUCT"));
                    return false;
                  } else {
                    return true;
                  }
                })
            .map(
                record -> {
                  var value = (Struct) record.value();
                  var orderedValues = new Object[columns.size()];
                  for (Field f : record.valueSchema().fields()) {
                    orderedValues[columns.get(f.name())] = value.get(f);
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
    var info = getApi().waitForJob(response.get(), BulkApiClient.URI_INGEST_JOB_INFO);
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

    buffer.clear();
  }

  @Override
  public void stop() {
    buffer.clear();
    api = null;
    LOG.info("Stop Salesforce sink task");
  }
}
