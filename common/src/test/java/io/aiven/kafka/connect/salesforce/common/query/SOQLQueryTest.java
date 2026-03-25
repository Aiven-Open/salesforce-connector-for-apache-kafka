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
package io.aiven.kafka.connect.salesforce.common.query;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SOQLQueryTest {

	@MethodSource("queryStrings")
	@ParameterizedTest
	void fromQueryString(String query, boolean isValid) {
		SOQLQuery soqlQuery = SOQLQuery.fromQueryString(query);

		assertEquals(isValid, soqlQuery.validate());
		assertEquals(query, soqlQuery.getQueryString(null));
	}

	private static Stream<Arguments> queryStrings() {
		return Stream.of(Arguments.of("SELECT Id,LastModifiedDate FROM Account", true),
				Arguments.of("SELECT Id,LastModifiedDate FROM User LIMIT 200", true),
				Arguments.of("SELECT FIELDS(STANDARD) FROM Contact LIMIT 200", true),
				Arguments.of(
						"SELECT FIELDS(STANDARD) FROM Contact WHERE Id = '003R000000ATjnCIAT' OR Id = '003R000000AZFUIIA5' OR Id = '003R000000DkYoFIAV'",
						true),
				Arguments.of(
						"SELECT Name,LastModifiedDate FROM Account WHERE BillingState IN ('California', 'New York')",
						true),
				Arguments.of("SELECT Id, Name,LastModifiedDate FROM Account WHERE Parent.Name = 'myaccount'", true),
				Arguments.of(
						"SELECT Title,LastModifiedDate FROM KnowledgeArticleVersion WHERE PublishStatus='online' WITH DATA CATEGORY Geography__c ABOVE usa__c",
						true),
				Arguments.of("SELECT LeadSource,LastModifiedDate FROM Lead GROUP BY LeadSource", true),
				Arguments.of(
						"SELECT LeadSource, COUNT(Name) cnt,LastModifiedDate FROM Lead GROUP BY ROLLUP(LeadSource)",
						true),
				Arguments.of(
						"SELECT LeadSource, Rating, GROUPING(LeadSource) grpLS, GROUPING(Rating) grpRating, COUNT(Name) cnt,LastModifiedDate FROM Lead GROUP BY ROLLUP(LeadSource, Rating)",
						true),
				Arguments.of(
						"SELECT LeadSource, COUNT(Name),LastModifiedDate FROM Lead GROUP BY LeadSource HAVING COUNT(Name) > 100",
						true),
				Arguments.of("SELECT Name, Industry,LastModifiedDate FROM Account ORDER BY Industry, Id", true),
				Arguments.of("SELECT Name,LastModifiedDate FROM Account WHERE Industry = 'Media' LIMIT 125", true),
				Arguments.of("SELECT Name,LastModifiedDate FROM Account WHERE Industry = 'Media' LIMIT 125", true),
				Arguments.of("SELECT Id, Name, Industry FROM Account ORDER BY Industry, Id", false),
				Arguments.of(
						"SELECT Id,LastModifiedDate, Name, Industry FROM Account WHERE LastModifiedDate > 2026 ORDER BY Industry, Id",
						false));
	}

}
