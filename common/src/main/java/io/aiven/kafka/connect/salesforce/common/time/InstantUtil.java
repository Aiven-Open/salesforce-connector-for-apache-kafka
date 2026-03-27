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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * This class handles the valid Date and DateTime formats supported by Salesforce. It provides a
 * number of utilities to work with these times and also format the output correctly for Salesforce
 * All times are kept in UTC
 */
public final class InstantUtil {
  /**
   * The format supported by the Salesforce API for querying, it should be used in all locations
   * where a date time may end up going to Salesforce
   */
  private static DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  /** This is a default constructor nothing is required here */
  private InstantUtil() {
    // nothing to do
  }

  /**
   * Returns a Instant from a time String with millisecond precision
   *
   * @param timeString The time that needs to be transformed into a Instant
   * @return A Instant with millisecond precision
   */
  public static Instant parseString(String timeString) {
    return Instant.parse(timeString).truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Get a Instant of the currentTime with millisecond precision using the UTC zoneId
   *
   * @return a Instant of the currentTime with millisecond precision using the UTC zoneId
   */
  public static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Compare a Instant with a time String and get the earliest time returned
   *
   * @param time1 The first time string to be compared
   * @param time2 The Instant to be compared
   * @return The earliest time as a Instant
   */
  public static Instant getEarliest(String time1, Instant time2) {
    Instant parsedTime1 = parseString(time1);
    if (parsedTime1.isAfter(time2)) {
      return time2;
    } else {
      return parsedTime1;
    }
  }

  /**
   * Compare a Instant with a time String and get the latest time returned
   *
   * @param time1 The first time string to be compared
   * @param time2 The Instant to be compared
   * @return The latest time as an Instant
   */
  public static Instant getLatest(String time1, Instant time2) {
    Instant parsedTime1 = parseString(time1);
    if (parsedTime1.isAfter(time2)) {
      return parsedTime1;
    } else {
      return time2;
    }
  }

  /**
   * From a list of time Strings return the oldest time
   *
   * @param times An array of Time Strings in the format of yyyy-MM-dd'T'HH:mm:ss.SSS'Z' for
   *     comparison
   * @return The oldest time from the list of times given
   */
  public static Instant min(String... times) {
    Instant earliestTime = null;
    for (String time : times) {
      Instant comparison = parseString(time);
      if (earliestTime == null) {
        earliestTime = comparison;
        continue;
      }
      earliestTime = earliestTime.isAfter(comparison) ? comparison : earliestTime;
    }
    return earliestTime;
  }

  /**
   * From a list of time Strings return the latest time
   *
   * @param times An array of Time Strings in the format of yyyy-MM-dd'T'HH:mm:ss.SSS'Z' for
   *     comparison
   * @return The latest time from the list of times given
   */
  public static Instant max(String... times) {
    Instant latestTime = null;
    for (String time : times) {
      Instant comparison = parseString(time);
      if (latestTime == null) {
        latestTime = comparison;
        continue;
      }
      latestTime = latestTime.isBefore(comparison) ? comparison : latestTime;
    }
    return latestTime;
  }

  /**
   * Get the Instant in a String format that is supported by the Salesforce api as toString will
   * return the smallest possible string version of the time.
   *
   * @param time The Instant that is to be transformed into a String
   * @return A String representation that will be returned with millisecond precision int the format
   *     of "yyyy-MM-ddTHH:mm:ss.SSSZ"
   */
  public static String toMilliString(Instant time) {
    return formatter.format(time); // TODO fix this
  }
}
