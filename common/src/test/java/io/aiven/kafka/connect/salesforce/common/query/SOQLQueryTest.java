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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SOQLQueryTest {
  private static String LAST_MODIFIED_DATE = "2025-11-08T00:00:00Z";

  @MethodSource("queryStrings")
  @ParameterizedTest
  void fromQueryString(String query, String expectedQuery, boolean isValid) {
    SOQLQuery soqlQuery = SOQLQuery.fromQueryString(query);

    assertEquals(isValid, soqlQuery.validate());
    // Everything gets appended with the Order By statement
    if (isValid) {
      assertEquals(expectedQuery, soqlQuery.getQueryString(null, false));
      assertEquals(expectedQuery, soqlQuery.getQueryString(null, true));
    }
  }

  @MethodSource("lastModifiedIncludedQueryStrings")
  @ParameterizedTest
  void fromQueryStringWithLastModifiedDate(String query, String expectedQuery, boolean isValid) {
    SOQLQuery soqlQuery = SOQLQuery.fromQueryString(query);

    assertEquals(isValid, soqlQuery.validate());
    // Everything gets appended with the Order By statement
    if (isValid) {
      assertEquals(expectedQuery, soqlQuery.getQueryString(LAST_MODIFIED_DATE, false));
    }
  }

  private static Stream<Arguments> queryStrings() {
    return Stream.of(
        Arguments.of(
            "SELECT Id,LastModifiedDate FROM Account",
            "SELECT Id,LastModifiedDate FROM Account ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Id,LastModifiedDate FROM User LIMIT 200",
            "SELECT Id,LastModifiedDate FROM User ORDER BY LastModifiedDate ASC LIMIT 200",
            true),
        Arguments.of(
            "SELECT FIELDS(STANDARD) FROM Contact LIMIT 200",
            "SELECT FIELDS(STANDARD) FROM Contact ORDER BY LastModifiedDate ASC LIMIT 200",
            true),
        Arguments.of(
            "SELECT FIELDS(STANDARD) FROM Contact WHERE Id = '003R000000ATjnCIAT' OR Id = '003R000000AZFUIIA5' OR Id = '003R000000DkYoFIAV'",
            "SELECT FIELDS(STANDARD) FROM Contact WHERE Id = '003R000000ATjnCIAT' OR Id = '003R000000AZFUIIA5' OR Id = '003R000000DkYoFIAV' ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Name,LastModifiedDate FROM Account WHERE BillingState IN ('California', 'New York')",
            "SELECT Name,LastModifiedDate FROM Account WHERE BillingState IN ('California', 'New York') ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Id, Name,LastModifiedDate FROM Account WHERE Parent.Name = 'myaccount'",
            "SELECT Id, Name,LastModifiedDate FROM Account WHERE Parent.Name = 'myaccount' ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Title,LastModifiedDate FROM KnowledgeArticleVersion WHERE PublishStatus='online' WITH DATA CATEGORY Geography__c ABOVE usa__c",
            "SELECT Title,LastModifiedDate FROM KnowledgeArticleVersion WHERE PublishStatus='online' WITH DATA CATEGORY Geography__c ABOVE usa__c ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT LeadSource,LastModifiedDate FROM Lead GROUP BY LeadSource",
            "SELECT LeadSource,LastModifiedDate FROM Lead GROUP BY LeadSource ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT LeadSource, COUNT(Name) cnt,LastModifiedDate FROM Lead GROUP BY ROLLUP(LeadSource)",
            "SELECT LeadSource, COUNT(Name) cnt,LastModifiedDate FROM Lead GROUP BY ROLLUP(LeadSource) ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT LeadSource, Rating, GROUPING(LeadSource) grpLS, GROUPING(Rating) grpRating, COUNT(Name) cnt,LastModifiedDate FROM Lead GROUP BY ROLLUP(LeadSource, Rating)",
            "SELECT LeadSource, Rating, GROUPING(LeadSource) grpLS, GROUPING(Rating) grpRating, COUNT(Name) cnt,LastModifiedDate FROM Lead GROUP BY ROLLUP(LeadSource, Rating) ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT LeadSource, COUNT(Name),LastModifiedDate FROM Lead GROUP BY LeadSource HAVING COUNT(Name) > 100",
            "SELECT LeadSource, COUNT(Name),LastModifiedDate FROM Lead GROUP BY LeadSource HAVING COUNT(Name) > 100 ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Name, Industry,LastModifiedDate FROM Account ",
            "SELECT Name, Industry,LastModifiedDate FROM Account ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Name,LastModifiedDate FROM Account WHERE Industry = 'Media' LIMIT 125",
            "SELECT Name,LastModifiedDate FROM Account WHERE Industry = 'Media' ORDER BY LastModifiedDate ASC LIMIT 125",
            true),
        Arguments.of(
            "SELECT Name,LastModifiedDate FROM Account WHERE Industry = 'Media' LIMIT 125",
            "SELECT Name,LastModifiedDate FROM Account WHERE Industry = 'Media' ORDER BY LastModifiedDate ASC LIMIT 125",
            true),
        Arguments.of("SELECT Id, Name, Industry FROM Account ORDER BY Industry, Id", null, false),
        Arguments.of(
            "SELECT Id,LastModifiedDate, Name, Industry FROM Account WHERE LastModifiedDate > 2026",
            null,
            false));
  }

  private static Stream<Arguments> lastModifiedIncludedQueryStrings() {
    return Stream.of(
        Arguments.of(
            "SELECT Id,LastModifiedDate FROM Account",
            "SELECT Id,LastModifiedDate FROM Account WHERE  LastModifiedDate > 2025-11-08T00:00:00Z ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Id,LastModifiedDate FROM User LIMIT 200",
            "SELECT Id,LastModifiedDate FROM User WHERE  LastModifiedDate > 2025-11-08T00:00:00Z ORDER BY LastModifiedDate ASC LIMIT 200",
            true),
        Arguments.of(
            "SELECT FIELDS(STANDARD) FROM Contact LIMIT 200",
            "SELECT FIELDS(STANDARD) FROM Contact WHERE  LastModifiedDate > 2025-11-08T00:00:00Z ORDER BY LastModifiedDate ASC LIMIT 200",
            true),
        Arguments.of(
            "SELECT FIELDS(STANDARD) FROM Contact WHERE Id = '003R000000ATjnCIAT' OR Id = '003R000000AZFUIIA5' OR Id = '003R000000DkYoFIAV'",
            "SELECT FIELDS(STANDARD) FROM Contact WHERE Id = '003R000000ATjnCIAT' OR Id = '003R000000AZFUIIA5' OR Id = '003R000000DkYoFIAV' AND LastModifiedDate > 2025-11-08T00:00:00Z ORDER BY LastModifiedDate ASC",
            true),
        Arguments.of(
            "SELECT Name,LastModifiedDate FROM Account WHERE BillingState IN ('California', 'New York')",
            "SELECT Name,LastModifiedDate FROM Account WHERE BillingState IN ('California', 'New York') AND LastModifiedDate > 2025-11-08T00:00:00Z ORDER BY LastModifiedDate ASC",
            true));
  }
}
