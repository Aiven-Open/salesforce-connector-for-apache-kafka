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

import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.kafka.connect.salesforce.BulkApiQueryEngine;
import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BulkApiSourceDataTest {

	public static final String SELECT_FIELD_STANDARD_FROM_ACCOUNT = "SELECT FIELD(STANDARD) FROM Account";
	private BulkApiSourceData sourceData;
	private OffsetManager offsetManager;
	private BulkApiQueryEngine engine;
	private SalesforceSourceConfig config;
	private final Map<String, String> executionMap = new HashMap<>();
	@BeforeEach
	void setup() {
		offsetManager = Mockito.mock(OffsetManager.class);
		config = Mockito.mock(SalesforceSourceConfig.class);
		engine = Mockito.mock(BulkApiQueryEngine.class);
		executionMap.put(SELECT_FIELD_STANDARD_FROM_ACCOUNT, ZonedDateTime.now(ZoneId.of("UTC")).toString());
	}
	@Test
	void testDelayInBetweenRequests() {
		Iterator<BulkApiNativeInfo> retval = Collections.emptyIterator();
		when(config.getBulkApiQueries()).thenReturn(List.of(SELECT_FIELD_STANDARD_FROM_ACCOUNT));
		when(config.getMinimumQueryExecutionDelay()).thenReturn(Duration.ofSeconds(5));
		when(engine.getRecords(any(SOQLQuery.class), eq(executionMap.get(SELECT_FIELD_STANDARD_FROM_ACCOUNT))))
				.thenReturn(retval);
		sourceData = new BulkApiSourceData(config, offsetManager, engine, executionMap);
		Iterator<BulkApiNativeInfo> iterator = sourceData.getSalesforceBulkApiStream().iterator();
		assertTrue(iterator.hasNext());
		assertEquals(iterator.next(), retval);
	}
}
