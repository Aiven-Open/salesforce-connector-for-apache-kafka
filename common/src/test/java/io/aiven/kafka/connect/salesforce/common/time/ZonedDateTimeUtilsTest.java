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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class ZonedDateTimeUtilsTest {

  @Test
  void parseZonedDateTimes() {
    String timeString = "2026-03-25T10:03:00.000Z";
    ZonedDateTime time = ZonedDateTimeUtil.parseString(timeString);
  }

  @Test
  void givenZonedDateTimeConvertBackToExactSameString() {
    String timeString = "2026-03-25T10:03:00.000Z";
    ZonedDateTime time = ZonedDateTimeUtil.parseString(timeString);
    String timeToMilliPrecision = ZonedDateTimeUtil.toMilliString(time);
    assertEquals(timeString, timeToMilliPrecision);
  }

  @Test
  void whenGivenTwoTimesCheckWhichIsTheLatest() {
    String timeString = "2026-03-25T10:03:00.000Z";
    ZonedDateTime time = ZonedDateTimeUtil.parseString(timeString);
    String latestTimeString = "2026-03-25T10:03:00.011Z";
    ZonedDateTime latestTime = ZonedDateTimeUtil.getlatest(latestTimeString, time);
    // We change all the time formats back to String for comparison because the default behaviour is
    // for the ZonedDAteTime to truncate
    // all 0's so if millis and seconds are all zeros they end up like
    // 2026-03-25T10:03 instead of 2026-03-25T10:03:00.000Z
    assertNotEquals(
        ZonedDateTimeUtil.toMilliString(latestTime), ZonedDateTimeUtil.toMilliString(time));
    ZonedDateTime newLatestTime = ZonedDateTimeUtil.getlatest(timeString, latestTime);
    assertEquals(
        ZonedDateTimeUtil.toMilliString(latestTime),
        ZonedDateTimeUtil.toMilliString(newLatestTime));
  }

  @Test
  void whenGivenTwoTimesCheckWhichIsTheEarliest() {
    String timeString = "2026-03-25T10:03:00.022Z";
    ZonedDateTime time = ZonedDateTimeUtil.parseString(timeString);
    String latestTimeString = "2026-03-25T10:03:00.033Z";
    ZonedDateTime latestTime = ZonedDateTimeUtil.getEarliest(latestTimeString, time);
    // We change all the time formats back to String for comparison because the default behaviour is
    // for the ZonedDAteTime to truncate
    // all 0's so if millis and seconds are all zeros they end up like
    // 2026-03-25T10:03 instead of 2026-03-25T10:03:00.000Z
    assertEquals(
        ZonedDateTimeUtil.toMilliString(latestTime), ZonedDateTimeUtil.toMilliString(time));
    ZonedDateTime earliestTime = ZonedDateTimeUtil.getEarliest(timeString, time);
    assertEquals(
        ZonedDateTimeUtil.toMilliString(time), ZonedDateTimeUtil.toMilliString(earliestTime));
    assertNotEquals(latestTimeString, ZonedDateTimeUtil.toMilliString(earliestTime));
  }
}
