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
package io.aiven.kafka.connect.salesforce.source.model;

import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.commons.kafka.connector.source.AbstractSourceNativeInfo;
import io.aiven.commons.kafka.connector.source.task.Context;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.common.bulk.model.SalesforceContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** BulkApiNativeInfo is an implementation of AbstractSourceNativeInfo for the Bulk Api. */
public class BulkApiNativeInfo extends AbstractSourceNativeInfo<BulkApiKey, String> {
  private final String topic;
  private final Integer partition;
  private final Long offset;
  private final String jobId;
  private final int totalRecords;
  private final String lastModifiedDate;
  private final String locator;

  /**
   * Constructor.
   *
   * @param nativeInfo the native info to process.
   * @param topic The name of the topic to produce the event to
   * @param partition The partition id to produce the event to
   * @param offset The offset id to produce the event to
   * @param jobId The job Id that the records are retrieved from
   * @param locator The locator for a specific page of data, should be the current page that is
   *     being processed not the next page
   * @param totalRecords The total number of records in the page of results
   * @param lastModifiedDate The lastModifiedDate used in the creation of this job
   */
  public BulkApiNativeInfo(
      final NativeInfo<BulkApiKey, String> nativeInfo,
      final String topic,
      final Integer partition,
      final Long offset,
      final String jobId,
      final String locator,
      final int totalRecords,
      final String lastModifiedDate) {
    super(nativeInfo);
    this.topic = topic;
    this.partition = partition;
    this.offset = offset;
    this.jobId = jobId;
    this.totalRecords = totalRecords;
    this.lastModifiedDate = lastModifiedDate;
    this.locator = locator;
  }

  /**
   * Creates the context for the native info.
   *
   * @return the context for the native Info.
   */
  @Override
  public Context getContext() {
    SalesforceContext ctx = new SalesforceContext(nativeKey());
    ctx.setTopic(topic);
    ctx.setPartition(partition);
    ctx.setOffset(offset);
    ctx.setJobId(jobId);
    ctx.setTotalRecords(totalRecords);
    ctx.setLastModifiedTimestamp(lastModifiedDate);
    ctx.setLocator(locator);
    return ctx;
  }

  /**
   * Read the input data from the nativeInfo.
   *
   * @return the input stream
   * @throws UnsupportedOperationException on Unsupported operation error.
   * @throws UnsupportedOperationException if the underlying NativeInfo does not support input
   *     streams.
   */
  @Override
  protected InputStream getInputStream() throws UnsupportedOperationException {
    return new ByteArrayInputStream(nativeInfo.nativeItem().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Gets an estimate of the input stream length.
   *
   * @return an estimate of the input stream length, or {@link #UNKNOWN_STREAM_LENGTH} if not known.
   * @throws UnsupportedOperationException if the underlying NativeInfo does not support input
   *     streams.
   */
  @Override
  public long estimateInputStreamLength() throws UnsupportedOperationException {
    return nativeInfo.nativeItem().length();
  }
}
