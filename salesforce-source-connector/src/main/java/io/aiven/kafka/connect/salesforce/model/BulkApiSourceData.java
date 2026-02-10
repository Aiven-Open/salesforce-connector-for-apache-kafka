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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aiven.commons.kafka.connector.source.NativeSourceData;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.task.Context;

import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.utils.SalesforceOffsetManagerEntry;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.function.IOSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This BulkApiSourceData facilitates sending BulkApi data into a SourceRecord
 * along with creating the OffsetManager entry for it.
 */
public class BulkApiSourceData
		implements
			NativeSourceData<String, List<CSVRecord>, SalesforceOffsetManagerEntry, BulkApiSourceRecord> {

	/**
	 * This deliminator is used to identify the API the data came from so that it is
	 * not mixed with data from other data streams from Salesforce
	 */
	private static final String BULK_API_TOPIC_DELIMINATOR = ".bulkapi.";
	private final List<CSVRecord> record;
	private final String queryExecutionTime;
	private final String objectName;
	private final SalesforceConfigFragment configFragment;
	private final ObjectMapper mapper = new ObjectMapper();
	private final byte[] lineSeparator = System.lineSeparator().getBytes(Charset.defaultCharset());

	/**
	 * Bulk Api Source Record
	 * 
	 * @param csvRecords
	 *            BulkApiResult that contains all the data
	 * @param objectName
	 *            The name of the object that was queried as part of this
	 * @param queryExecutionTime
	 *            The time the query was executed at
	 * @param configFragment
	 *            The SalesforceConfigFragment with all the relevant config for
	 *            configuring the BulkApiSourceData
	 */
	public BulkApiSourceData(final List<CSVRecord> csvRecords, final String objectName, String queryExecutionTime,
			final SalesforceConfigFragment configFragment) {
		this.record = csvRecords;
		this.queryExecutionTime = queryExecutionTime;
		this.objectName = objectName;
		this.configFragment = configFragment;
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
	 * @param offset
	 *            the native key to start from. May be {@code null} ot indicate *
	 *            start at the beginning.
	 * @return A stream of native objects. May be empty but not {@code null}.
	 */
	@Override
	public Stream<List<CSVRecord>> getNativeItemStream(String offset) {
		return Stream.of(record);
	}

	/**
	 * Get an inputStream for a SourceRecord
	 * 
	 * @param bulkApiSourceRecord
	 * 
	 *            This is the BulkApiSourceRecord that contains all the information
	 *            required to construct the stream of records and offsets
	 * @return An IOSupplier of csvRecords that have been transformed into maps
	 */
	@Override
	public IOSupplier<InputStream> getInputStream(BulkApiSourceRecord bulkApiSourceRecord) {
		byte[] allRecords = new byte[4096];
		ByteBuffer buffer = ByteBuffer.wrap(allRecords);

		bulkApiSourceRecord.getNativeItem().forEach(record -> {
			try {
				buffer.put(mapper.writeValueAsBytes(record.toMap()));
				//Seperate out each record
				buffer.put(lineSeparator);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		});
		return () -> new ByteArrayInputStream(buffer.array());
	}

	/**
	 * Get the Native Key from a bulkApiResult
	 * 
	 * @param bulkApiResult
	 *            a bulkApiResult which has its own key etc inside
	 * @return The NativeKey
	 */
	@Override
	public String getNativeKey(List<CSVRecord> bulkApiResult) {
		// TODO is this right?
		// It looks like it should be more unique perhaps like
		// the offset managment key
		return objectName;
	}

	/**
	 * Get the native key
	 *
	 * @param key
	 *            The Native key
	 * @return a parsed native key
	 */
	@Override
	public String parseNativeKey(String key) {
		return key;
	}

	/**
	 * Creates a BulkApiSourceRecord
	 *
	 * @param csvRecord
	 *            a CSVRecord
	 * @return Create a BulkApiSourceRecord
	 */
	@Override
	public BulkApiSourceRecord createSourceRecord(List<CSVRecord> csvRecord) {
		return new BulkApiSourceRecord(csvRecord, getNativeKey(csvRecord));
	}

	/**
	 * Create a SalesforceOffsetManagerEntry
	 *
	 * @param csvRecord
	 *            This needs to be updated
	 * @return SalesforceOffsetManagerEntry
	 */
	@Override
	public SalesforceOffsetManagerEntry createOffsetManagerEntry(List<CSVRecord> csvRecord) {
		return new SalesforceOffsetManagerEntry(getSourceName(), objectName, queryExecutionTime);
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
		return SalesforceOffsetManagerEntry.asKey(getSourceName(), objectName);
	}

	/**
	 * Returns the context if available in the record it determines the topic the
	 * data is sent to and if particular options in the context are set can also
	 * determine the partition the record is sent to
	 *
	 * @param record
	 *            This is an individual BulkApiResult
	 * @return context if available reutnrs an empty Optional if not
	 */
	@Override
	public Optional<Context<String>> extractContext(List<CSVRecord> record) {
		Context<String> context = new Context<>(getNativeKey(record));
		context.setTopic(configFragment.getTopicPrefix() + BULK_API_TOPIC_DELIMINATOR + objectName);
		context.setStorageKey(getNativeKey(record));
		context.setPartition(null);
		context.setStorageKey(objectName);

		return Optional.of(context);
	}
}
