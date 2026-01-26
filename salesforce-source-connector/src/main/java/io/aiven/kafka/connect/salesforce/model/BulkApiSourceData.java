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

import io.aiven.commons.kafka.connector.source.NativeSourceData;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.task.Context;

import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.function.IOSupplier;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This BulkApiSourceData facilitates sending BulkApi data into a SourceRecord
 * along with creating the OffsetManager entry for it.
 */
public class BulkApiSourceData
		implements
			NativeSourceData<String, CSVRecord, SalesforceOffsetManagerEntry, BulkApiSourceRecord> {

	/**
	 * Default constructor
	 */
	public BulkApiSourceData() {

	}
	/**
	 * get the source name from the data
	 * 
	 * @return the source name
	 */
	@Override
	public String getSourceName() {
		return "Salesforce Bulk API";
	}

	/**
	 * Get the native Item in a stream
	 * 
	 * @param s
	 *            This needs to be updated
	 * @return A stream of data
	 */
	@Override
	public Stream<CSVRecord> getNativeItemStream(String s) {
		return Stream.empty();
	}

	/**
	 * Get an inputStream for a SourceRecord
	 * 
	 * @param bulkApiSourceRecord
	 *            This needs to be updated
	 * @return This needs to be updated
	 */
	@Override
	public IOSupplier<InputStream> getInputStream(BulkApiSourceRecord bulkApiSourceRecord) {
		return null;
	}

	/**
	 * Get the Native Key
	 * 
	 * @param o
	 *            This needs to be updated
	 * @return The NativeKey
	 */
	@Override
	public String getNativeKey(CSVRecord o) {
		return "";
	}

	/**
	 * Get the native key
	 * 
	 * @param s
	 *            This needs to be updated
	 * @return a parsed native key
	 */
	@Override
	public String parseNativeKey(String s) {
		return s;
	}

	/**
	 * Creates a BulkApiSourceRecord
	 * 
	 * @param o
	 *            This needs to be updated
	 * @return Create a sourceRecord
	 */
	@Override
	public BulkApiSourceRecord createSourceRecord(CSVRecord o) {
		return null;
	}

	/**
	 * Create a SalesforceOffsetManagerEntry
	 * 
	 * @param o
	 *            This needs to be updated
	 * @return SalesforceOffsetManagerEntry
	 */
	@Override
	public SalesforceOffsetManagerEntry createOffsetManagerEntry(CSVRecord o) {
		return null;
	}

	/**
	 * Get the OffsetManagerKey
	 * 
	 * @param s
	 *            This needs to be updated
	 * @return The offsetManager key
	 */
	@Override
	public OffsetManager.OffsetManagerKey getOffsetManagerKey(String s) {
		return null;
	}

	/**
	 * Returns the context if available in the record
	 *
	 * @param records
	 *            This is an individual CSVRecord
	 * @return context if available reutnrs an empty Optional if not
	 */
	@Override
	public Optional<Context<String>> extractContext(CSVRecord records) {
		return Optional.empty();
	}
}
