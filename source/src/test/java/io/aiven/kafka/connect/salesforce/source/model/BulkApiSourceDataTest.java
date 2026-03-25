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
package io.aiven.kafka.connect.salesforce.source.model;

import io.aiven.commons.kafka.connector.source.EvolvingSourceRecord;
import io.aiven.commons.kafka.connector.source.EvolvingSourceRecordIterator;
import io.aiven.commons.kafka.connector.source.OffsetManager;
import io.aiven.commons.kafka.connector.source.config.SourceConfigFragment;
import io.aiven.commons.kafka.connector.source.transformer.CsvTransformer;
import io.aiven.kafka.connect.salesforce.common.bulk.model.SalesforceContext;
import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import io.aiven.kafka.connect.salesforce.source.BulkApiQueryEngine;
import io.aiven.kafka.connect.salesforce.source.config.SalesforceSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
	@BeforeEach
	void setup() {
		offsetManager = Mockito.mock(OffsetManager.class);
		config = Mockito.mock(SalesforceSourceConfig.class);
		engine = Mockito.mock(BulkApiQueryEngine.class);

	}

}
