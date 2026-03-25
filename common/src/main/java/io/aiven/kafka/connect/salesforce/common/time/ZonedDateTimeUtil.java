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
package io.aiven.kafka.connect.salesforce.common.time;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * This class handles the valid Date and DateTime formats supported by Salesforce. It provides a
 * number of utilities to work with these times and also format the output correctly for Salesforce
 * All times are kept in UTC
 */
public class ZonedDateTimeUtil {
  private static final String UTC = "UTC";

  /**
   * The format supported by the Salesforce API for querying, it should be used in all locations
   * where a date time may end up going to Salesforce
   */
  private static DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /** This is a default constructor nothing is required here */
  public ZonedDateTimeUtil() {
    // nothing to do
  }

  /**
   * Returns a ZonedDateTime from a time string String with millisecond precision
   *
   * @param timeString The time that needs to be transformed into a ZonedDateTime
   * @return A ZonedDateTime with millisecond precision
   */
  public static ZonedDateTime parseString(String timeString) {
    return ZonedDateTime.parse(timeString).truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Get a ZonedDateTime of the currentTime with millisecond precision using the UTC zoneId
   *
   * @return a ZonedDateTime of the currentTime with millisecond precision using the UTC zoneId
   */
  public static ZonedDateTime now() {
    return ZonedDateTime.now(ZoneId.of(UTC)).truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Compare a ZonedDateTime with a time String and get the earliest time returned
   *
   * @param time1 The first time string to be compared
   * @param time2 The ZonedDateTime to be compared
   * @return The earliest time as a ZonedDateTime
   */
  public static ZonedDateTime getEarliest(String time1, ZonedDateTime time2) {
    ZonedDateTime parsedTime1 = parseString(time1);
    if (parsedTime1.isAfter(time2)) {
      return time2;
    } else {
      return parsedTime1;
    }
  }

  /**
   * Compare a ZonedDateTime with a time String and get the latest time returned
   *
   * @param time1 The first time string to be compared
   * @param time2 The ZonedDateTime to be compared
   * @return The latest time as a ZonedDateTime
   */
  public static ZonedDateTime getlatest(String time1, ZonedDateTime time2) {
    ZonedDateTime parsedTime1 = parseString(time1);
    if (parsedTime1.isAfter(time2)) {
      return parsedTime1;
    } else {
      return time2;
    }
  }

  /**
   * Get the ZonedDateTime in a String format that is suported by the Salesforce api as toString
   * will return the smallest possible string version of the time.
   *
   * @param time The ZonedDateTime that is to be transformed into a String
   * @return A String representation that will be returned with millisecond precision int the format
   *     of "yyyy-MM-ddTHH:mm:ss.SSSZ"
   */
  public static String toMilliString(ZonedDateTime time) {
    return time.format(formatter);
  }
}
