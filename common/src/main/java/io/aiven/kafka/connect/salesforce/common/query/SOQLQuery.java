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

import java.util.List;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows the deconstruction and reconstruction of a SOQL query for use in the querying
 * of data. Allows better control and validation of SOQL queries by adding specifically supported
 * SOQL keywords.
 */
public class SOQLQuery {
  private static final String FIELDS_STANDARD = "FIELDS(STANDARD)";
  private static final String LAST_MODIFIED_DATE = "LastModifiedDate";
  private static final Logger LOGGER = LoggerFactory.getLogger(SOQLQuery.class);
  private static final String SELECT = "SELECT";
  private static final String FROM = "FROM";
  private static final String WITH = "WITH";
  private static final String ORDER_BY = "ORDER BY";
  private static final String HAVING = "HAVING";
  private static final String GROUP_BY = "GROUP BY";
  private static final String OFFSET = "OFFSET";
  private static final String LIMIT = "LIMIT";
  private static final String WHERE = "WHERE";
  private static final List<String> SUPPORTED_KEYWORDS =
      List.of(SELECT, FROM, WHERE, HAVING, ORDER_BY, GROUP_BY, LIMIT, OFFSET, WITH);
  private String SOQLQuery;
  private String select;
  private String from;
  private String where;
  private String having;
  private String orderBy;
  private String groupBy;
  private String limit;
  private String with;
  private String offset;

  /** Default constructor for SOQLQuery */
  SOQLQuery() {
    // default construction
  }

  /**
   * Get the contents of the SELECT portion of the select statement
   *
   * @return Get the contents of the SELECT portion of the select statement
   */
  public String getSelect() {
    return select;
  }

  /**
   * Set the contents of the SELECT portion of the select statement
   *
   * @param select Set the contents of the SELECT portion of the select statement
   */
  public void setSelect(final String select) {
    this.select = select;
  }

  /**
   * Get the contents of the FROM portion of the select statement
   *
   * @return Get the contents of the FROM portion of the select statement
   */
  public String getFrom() {
    return from;
  }

  /**
   * Set the contents of the FROM portion of the select statement
   *
   * @param from Set the contents of the FROM portion of the select statement
   */
  public void setFrom(final String from) {
    this.from = from;
  }

  /**
   * Get the contents of the WHERE portion of the select statement
   *
   * @return Get the contents of the WHERE portion of the select statement
   */
  public String getWhere() {
    return where;
  }

  /**
   * Set the contents of the WHERE portion of the select statement
   *
   * @param where Set the contents of the WHERE portion of the select statement
   */
  public void setWhere(final String where) {
    this.where = where;
  }

  /**
   * Get the contents of the HAVING portion of the select statement
   *
   * @return Get the contents of the HAVING portion of the select statement
   */
  public String getHaving() {
    return having;
  }

  /**
   * Set the contents of the HAVING portion of the select statement
   *
   * @param having Set the contents of the HAVING portion of the select statement
   */
  public void setHaving(final String having) {
    this.having = having;
  }

  /**
   * Get the contents of the ORDER BY portion of the select statement
   *
   * @return Get the contents of the ORDER BY portion of the select statement
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * Set the contents of the ORDER BY portion of the select statement
   *
   * @param orderBy Set the contents of the ORDER BY portion of the select statement
   */
  public void setOrderBy(final String orderBy) {
    this.orderBy = orderBy;
  }

  /**
   * Get the contents of the GROUP BY portion of the select statement
   *
   * @return Get the contents of the GROUP BY portion of the select statement
   */
  public String getGroupBy() {
    return groupBy;
  }

  /**
   * Set the contents of the GROUP BY portion of the select statement
   *
   * @param groupBy Set the contents of the GROUP BY portion of the select statement
   */
  public void setGroupBy(final String groupBy) {
    this.groupBy = groupBy;
  }

  /**
   * Get the contents of the LIMIT portion of the select statement
   *
   * @return Get the contents of the LIMIT portion of the select statement
   */
  public String getLimit() {
    return limit;
  }

  /**
   * Set the contents of the LIMIT portion of the select statement
   *
   * @param limit Set the contents of the LIMIT portion of the select statement
   */
  public void setLimit(final String limit) {
    this.limit = limit;
  }

  /**
   * Get the contents of the WITH portion of the select statement
   *
   * @return Get the contents of the WITH portion of the select statement
   */
  public String getWith() {
    return with;
  }

  /**
   * Set the contents of the WITH portion of the select statement
   *
   * @param with Set the contents of the WITH portion of the select statement
   */
  public void setWith(final String with) {
    this.with = with;
  }

  /**
   * Get the contents of the OFFSET portion of the select statement
   *
   * @return Get the contents of the OFFSET portion of the select statement
   */
  public String getOffset() {
    return offset;
  }

  /**
   * Set the contents of the OFFSET portion of the select statement
   *
   * @param offset Set the contents of the OFFSET portion of the select statement
   */
  public void setOffset(final String offset) {
    this.offset = offset;
  }

  /**
   * Get the original SOQL query string that was used to create this object
   *
   * @return Get the original SOQL query string that was used to create this object
   */
  public String getSOQLQuery() {
    return SOQLQuery;
  }

  /**
   * Set the original SOQL query that was supplied to create this Object
   *
   * @param SOQLQuery The original SOQL query that was supplied to create this Object
   */
  public void setSOQLQuery(final String SOQLQuery) {
    this.SOQLQuery = SOQLQuery;
  }

  /**
   * Generates a SOQL query string that can be used to retrieve information from Salesforce through
   * the bulk api. Ordering of SOQL query construction aligns with Salesforce documentation <a href=
   * "https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select.htm">https://developer.salesforce.com</a>
   *
   * @param lastModifiedDate The lastModifiedDate for the object to begin querying data from
   * @return A useable SOQL query
   */
  public String getQueryString(String lastModifiedDate) {
    return (getPart(SELECT, select)
            + getPart(FROM, from)
            + getWhere(where, lastModifiedDate)
            + getPart(WITH, with)
            + getPart(GROUP_BY, groupBy)
            + getPart(HAVING, having)
            + getPart(ORDER_BY, orderBy)
            + getPart(LIMIT, limit)
            + getPart(OFFSET, offset))
        .trim();
  }

  private String getWhere(String partInstruction, String lastModifiedDate) {

    String where = getPart(WHERE, partInstruction);

    if (StringUtils.isBlank(where)) {
      return getPart(
          WHERE, lastModifiedDate != null ? " LastModifiedDate > " + lastModifiedDate : null);
    } else {
      where += lastModifiedDate != null ? " LastModifiedDate > " + lastModifiedDate : "";
      return where;
    }
  }

  private static boolean isEmpty(String partInstruction) {
    return partInstruction == null || partInstruction.isEmpty();
  }

  private String getPart(String partName, String partInstruction) {
    return !isEmpty(partInstruction) ? partName + " " + partInstruction + " " : "";
  }

  /**
   * Validates that the SOQL query meets the expectations of the connector for it to be able to
   * track offsets correctly
   *
   * @return True if valid SOQL query for this connector, False if not valid for this connector
   */
  public boolean validate() {
    // Should not include the LastModifiedDate in the where clause so we can
    // manipulate it.
    if (!select.contains(FIELDS_STANDARD) && !select.contains(LAST_MODIFIED_DATE)) {
      return false;
    }
    // Should not include the LastModifiedDate in the where clause so we can
    // manipulate it.
    if (where != null && where.contains(LAST_MODIFIED_DATE)) {
      return false;
    }
    return true;
  }

  /**
   * Create a SOQLQuery Object from a query String
   *
   * @param queryString An original SOQL query
   * @return An SOQLQuery Object
   */
  public static SOQLQuery fromQueryString(String queryString) {
    return populateSOQLQuery(
        new SOQLQuery(), queryString.split("(" + buildSOQLQueryRegex() + ")"), queryString);
  }

  /**
   * @param query The SOQLQuery Object
   * @param soqlQueryParts The individual parts of the SOQL query broken by the supported tokens
   * @param queryString The original SOQL query String provided
   * @return A fully populated SOQL Query Object
   */
  private static SOQLQuery populateSOQLQuery(
      SOQLQuery query, String[] soqlQueryParts, String queryString) {
    for (int i = 0; i < soqlQueryParts.length; i++) {
      switch (soqlQueryParts[i]) {
        case SELECT:
          query.setSelect(soqlQueryParts[++i].trim());
          break;
        case FROM:
          query.setFrom(soqlQueryParts[++i].trim());
          break;
        case GROUP_BY:
          query.setGroupBy(soqlQueryParts[++i].trim());
          break;
        case ORDER_BY:
          query.setOrderBy(soqlQueryParts[++i].trim());
          break;
        case HAVING:
          query.setHaving(soqlQueryParts[++i].trim());
          break;
        case LIMIT:
          query.setLimit(soqlQueryParts[++i].trim());
          break;
        case OFFSET:
          query.setOffset(soqlQueryParts[++i].trim());
          break;
        case WHERE:
          query.setWhere(soqlQueryParts[++i].trim());
          break;
        case WITH:
          query.setWith(soqlQueryParts[++i].trim());
          break;
        default:
          LOGGER.warn("Unrecognized token {}, in SOQL query {}", soqlQueryParts[i], queryString);
          break;
      }
    }
    query.setSOQLQuery(queryString);
    return query;
  }

  /**
   * Builds a regex that will extract the constituent parts of a SOQL query so it can be properly
   * validated
   *
   * @return A regex String to split a SOQL query string
   */
  private static String buildSOQLQueryRegex() {
    String deliminatorStub = "((?<=%1$s)|(?=%1$s))";
    StringBuilder deliminator = new StringBuilder();
    for (String keyword : SUPPORTED_KEYWORDS) {
      if (!deliminator.isEmpty()) {
        // add OR separator
        deliminator.append("|");
      }
      deliminator.append(String.format(deliminatorStub, keyword));
    }
    return deliminator.toString();
  }
}
