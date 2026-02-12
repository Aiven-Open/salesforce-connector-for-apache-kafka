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
package io.aiven.kafka.connect.salesforce.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
//import io.aiven.kafka.connect.salesforce.transformers.CsvTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVTransformerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CSVTransformerTest.class);
	private SalesforceConfigFragment configFragment;
	private SalesforceSourceConfig commonConfig;

	// private CsvTransformer transformer;

	private ObjectMapper mapper = new ObjectMapper();

	// @Test
	// void stepThroughCSVTest() throws IOException {
	//
	// String csvRecords =
	// Files.readString(Paths.get("src/test/resources/values.csv"));
	//
	// configFragment = mock(SalesforceConfigFragment.class);
	// commonConfig = mock(SalesforceSourceConfig.class);
	// BulkApiSourceData data = new BulkApiSourceData(csvRecords, "values",
	// "12-02-2026", configFragment);
	// BulkApiSourceRecord sourceRecord = new BulkApiSourceRecord(csvRecords,
	// "values");
	// sourceRecord.setContext(data.extractContext(sourceRecord.getNativeItem()).orElseGet(null));
	// transformer = new CsvTransformer(commonConfig);
	//
	// transformer.generateRecords(data, sourceRecord).forEach(entry -> {
	// try {
	// LOGGER.info("Entry from csvTransformer {}",
	// mapper.writeValueAsString(entry));
	// } catch (JsonProcessingException e) {
	// throw new RuntimeException(e);
	// }
	// });
	//
	// }
}
