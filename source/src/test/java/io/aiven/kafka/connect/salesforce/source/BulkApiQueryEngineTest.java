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
package io.aiven.kafka.connect.salesforce.source;

import io.aiven.commons.kafka.connector.common.NativeInfo;
import io.aiven.kafka.connect.salesforce.common.bulk.BulkApiClient;
import io.aiven.kafka.connect.salesforce.common.bulk.model.BulkApiKey;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResult;
import io.aiven.kafka.connect.salesforce.common.bulk.query.BulkApiResultResponse;
import io.aiven.kafka.connect.salesforce.common.bulk.query.JobState;
import io.aiven.kafka.connect.salesforce.common.bulk.query.QueryResponse;
import io.aiven.kafka.connect.salesforce.common.query.SOQLQuery;
import io.aiven.kafka.connect.salesforce.source.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.source.model.BulkApiNativeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BulkApiQueryEngineTest {

	public static final String QUERY = "SELECT Id, Name, LastModifiedDate FROM Account";
	private BulkApiQueryEngine engine;
	private BulkApiClient apiClient;
	private SalesforceSourceConfig config;
	private BulkApiKey apiKey = new BulkApiKey("bulkApi", QUERY, null, "");
	@BeforeEach
	void startup() {
		apiClient = Mockito.mock(BulkApiClient.class);
		config = Mockito.mock(SalesforceSourceConfig.class);

	}

	@Test
	void getSinglePage() {
		String jobId = "ABCD123";
		engine = new BulkApiQueryEngine(config, apiClient);
		SOQLQuery query = SOQLQuery.fromQueryString(QUERY);

		when(apiClient.submitQueryJob(eq(query.getQueryString(null)))).thenReturn(Optional.of(jobId));
		when(apiClient.queryJobStatus(eq(jobId))).thenReturn(getJobStatus(jobId, JobState.JobComplete));
		when(apiClient.getJobResults(eq(jobId), eq(null), eq(null), any(BulkApiKey.class)))
				.thenReturn(CompletableFuture.completedFuture(getJobResults(null, 5)));

		Iterator<BulkApiNativeInfo> iterator = engine.getRecords(query, null);
		assertTrue(iterator.hasNext());

		BulkApiNativeInfo next = iterator.next();

		assertFalse(iterator.hasNext());
	}

	@Test
	void getMultiplePages() {
		String jobId = "ABCD123";
		engine = new BulkApiQueryEngine(config, apiClient);
		SOQLQuery query = SOQLQuery.fromQueryString(QUERY);

		when(apiClient.submitQueryJob(eq(query.getQueryString(null)))).thenReturn(Optional.of(jobId));
		when(apiClient.queryJobStatus(eq(jobId))).thenReturn(getJobStatus(jobId, JobState.JobComplete));
		when(apiClient.getJobResults(eq(jobId), eq(null), eq(null), any(BulkApiKey.class)))
				.thenReturn(CompletableFuture.completedFuture(getJobResults("Locator1", 5)));

		when(apiClient.getJobResults(eq(jobId), eq("Locator1"), eq(null), any(BulkApiKey.class)))
				.thenReturn(CompletableFuture.completedFuture(getJobResults("Locator2", 5)));

		when(apiClient.getJobResults(eq(jobId), eq("Locator2"), eq(null), any(BulkApiKey.class)))
				.thenReturn(CompletableFuture.completedFuture(getJobResults(null, 5)));

		Iterator<BulkApiNativeInfo> iterator = engine.getRecords(query, null);
		assertTrue(iterator.hasNext());

		BulkApiNativeInfo next = iterator.next();

		assertTrue(iterator.hasNext());
		next = iterator.next();

		assertTrue(iterator.hasNext());
		next = iterator.next();

		assertFalse(iterator.hasNext());
	}

	private BulkApiResultResponse getJobResults(String locator, int numberOfRecords) {
		BulkApiResultResponse response = new BulkApiResultResponse();
		response.setLocator(locator);
		response.setNumberOfRecords(numberOfRecords);
		response.setResult(getResults(numberOfRecords));
		return response;
	}

	private BulkApiResult getResults(int numberOfRecords) {
		BulkApiResult apiResult = new BulkApiResult(new NativeInfo<>(apiKey, ""), "Account");
		return apiResult;
	}

	private Optional<QueryResponse> getJobStatus(String jobId, JobState state) {
		QueryResponse queryResponse = new QueryResponse();
		queryResponse.setState(state);
		queryResponse.setId(jobId);
		return Optional.of(queryResponse);
	}
}
