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
package io.aiven.kafka.connect.salesforce.common.bulk.query;

import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;

/**
 * This is a holder for the response from the Bulk Api It allows the storage and processing of the
 * CSV file response.
 */
public final class BulkApiResult {

  /**
   * An object in Salesforce is the table name, the ObjectName is the object name from the query
   * submitted to the Salesforce bulk api.
   */
  private final String objectName;

  /** The native info for this result. */
  private final NativeInfo<BulkApiKey, String> nativeInfo;

  /**
   * This constructor allows you to create the object directly from the response received from the
   * API
   *
   * @param nativeInfo The native info for this result.
   * @param objectName the name of the object that the results came from
   */
  public BulkApiResult(final NativeInfo<BulkApiKey, String> nativeInfo, final String objectName) {
    this.nativeInfo = nativeInfo;
    this.objectName = objectName;
  }

  /**
   * Get the NativeInfo for this result.
   *
   * @return the NativeInfo for this result.
   */
  public NativeInfo<BulkApiKey, String> getNativeInfo() {
    return nativeInfo;
  }

  /**
   * Gets the BulkApiKey for this result.
   *
   * @return the BulkApiKey for this result.
   */
  public BulkApiKey getKey() {
    return nativeInfo.nativeKey();
  }

  /**
   * This is to retrieve the csv file contents
   *
   * @return The contents of the CSV file
   */
  public String getContents() {
    return nativeInfo.nativeItem();
  }

  /**
   * Get the name of the object these results are from
   *
   * @return the object name
   */
  public String getObjectName() {
    return objectName;
  }

  /**
   * Get the time the query was executed at
   *
   * @return time that the query was executed at
   */
  public String getQueryExecutionTime() {
    return nativeInfo.nativeKey().getLastExecutionTime();
  }

  /**
   * Get the size of the content stored in this result
   *
   * @return the size of the content stored in this result
   */
  public long getContentSize() {
    return nativeInfo.nativeItem() == null ? 0 : nativeInfo.nativeItem().length();
  }
}
