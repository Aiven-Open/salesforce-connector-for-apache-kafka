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
		return Stream.of(Arguments.of("SELECT Id FROM Account", false),
				Arguments.of("SELECT Id FROM User LIMIT 200", false),
				Arguments.of("SELECT FIELDS(ALL) FROM Contact LIMIT 200", true),
				Arguments.of(
						"SELECT FIELDS(ALL) FROM Contact WHERE Id = '003R000000ATjnCIAT' OR Id = '003R000000AZFUIIA5' OR Id = '003R000000DkYoFIAV'",
						true),
				Arguments.of("SELECT Name FROM Account WHERE BillingState IN ('California', 'New York')", false),
				Arguments.of("SELECT Id, Name FROM Account WHERE Parent.Name = 'myaccount'", false),
				Arguments.of(
						"SELECT Title FROM KnowledgeArticleVersion WHERE PublishStatus='online' WITH DATA CATEGORY Geography__c ABOVE usa__c",
						false),
				Arguments.of("SELECT LeadSource FROM Lead GROUP BY LeadSource", false),
				Arguments.of("SELECT LeadSource, COUNT(Name) cnt FROM Lead GROUP BY ROLLUP(LeadSource)", false),
				Arguments.of(
						"SELECT LeadSource, Rating, GROUPING(LeadSource) grpLS, GROUPING(Rating) grpRating, COUNT(Name) cnt FROM Lead GROUP BY ROLLUP(LeadSource, Rating)",
						false),
				Arguments.of("SELECT LeadSource, COUNT(Name) FROM Lead GROUP BY LeadSource HAVING COUNT(Name) > 100",
						false),
				Arguments.of("SELECT Name, Industry FROM Account ORDER BY Industry, Id", false),
				Arguments.of("SELECT Name FROM Account WHERE Industry = 'Media' LIMIT 125", false),
				Arguments.of("SELECT Name FROM Account WHERE Industry = 'Media' LIMIT 125", false),
				Arguments.of("SELECT Id,LastModifiedDate, Name, Industry FROM Account ORDER BY Industry, Id", true));
	}

}
