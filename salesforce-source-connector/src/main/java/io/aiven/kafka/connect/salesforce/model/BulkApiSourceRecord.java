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
package io.aiven.kafka.connect.salesforce.model;

import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.commons.kafka.connector.source.AbstractSourceRecord;
import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

/**
 * The Bulk Api Source record implements the AbstractSourceRecord. It allows the
 * AbstractSourceRecordIterator to process all the returned records from the
 * source. This implementation is specific to the Bulk Api for Salesforce.
 */
public class BulkApiSourceRecord
		extends
			AbstractSourceRecord<String, List<CSVRecord>, SalesforceOffsetManagerEntry, BulkApiSourceRecord> {

	/**
	 * The plain Object type here will be changed to one for the BulkApi response
	 * 
	 * @param record
	 *            A CSVRecord
	 * @param nativeKey
	 *            The native key or identifier for this record that comes from
	 *            Salesforce
	 */
	public BulkApiSourceRecord(List<CSVRecord> record, String nativeKey) {// NOPMD not used yet
		super(new NativeInfo<String, List<CSVRecord>>() {

			/**
			 * Returns the NativeItem
			 *
			 * @return the NativeItem
			 *
			 */
			@Override
			public List<CSVRecord> getNativeItem() {
				return record;
			}

			/**
			 * Returns the NativeKey
			 *
			 * @return the NativeKey
			 */
			@Override
			public String getNativeKey() {
				return nativeKey;
			}

			/**
			 * Get the size of the Native Item
			 *
			 * @return the size of the NativeItem
			 */
			@Override
			public long getNativeItemSize() {
				// TODO Double check this as it may be wrong, data is before the change into
				// maps so could be smaller then actual
				return record.size();
			}
		});
	}

	/**
	 * Constructor to take a BulkApiSourceRecord for duplication()
	 * 
	 * @param bulkApiSourceRecord
	 *            a duplicate BulkApiSourceRecord
	 */
	private BulkApiSourceRecord(final BulkApiSourceRecord bulkApiSourceRecord) {
		super(bulkApiSourceRecord);
	}

	/**
	 * Duplicates this record
	 * 
	 * @return duplicate record
	 */
	@Override
	public BulkApiSourceRecord duplicate() {
		return new BulkApiSourceRecord(this);
	}

}
