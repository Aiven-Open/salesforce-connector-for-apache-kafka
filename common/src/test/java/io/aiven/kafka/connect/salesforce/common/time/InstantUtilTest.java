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

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class InstantUtilTest {

  @Test
  void parseInstants() {
    String timeString = "2026-03-25T10:03:00.000Z";
    Instant time = InstantUtil.parseString(timeString);
  }

  @Test
  void givenInstantConvertBackToExactSameString() {
    String timeString = "2026-03-25T10:03:00.000Z";
    Instant time = InstantUtil.parseString(timeString);
    String timeToMilliPrecision = InstantUtil.toMilliString(time);
    assertEquals(timeString, timeToMilliPrecision);
  }

  @Test
  void whenGivenTwoTimesCheckWhichIsTheLatest() {
    String timeString = "2026-03-25T10:03:00.000Z";
    Instant time = InstantUtil.parseString(timeString);
    String latestTimeString = "2026-03-25T10:03:00.011Z";
    Instant latestTime = InstantUtil.getlatest(latestTimeString, time);
    // We change all the time formats back to String for comparison because the default behaviour is
    // for the Instant to truncate
    // all 0's so if millis and seconds are all zeros they end up like
    // 2026-03-25T10:03 instead of 2026-03-25T10:03:00.000Z
    assertNotEquals(InstantUtil.toMilliString(latestTime), InstantUtil.toMilliString(time));
    Instant newLatestTime = InstantUtil.getlatest(timeString, latestTime);
    assertEquals(InstantUtil.toMilliString(latestTime), InstantUtil.toMilliString(newLatestTime));
  }

  @Test
  void whenGivenTwoTimesCheckWhichIsTheEarliest() {
    String timeString = "2026-03-25T10:03:00.022Z";
    Instant time = InstantUtil.parseString(timeString);
    String latestTimeString = "2026-03-25T10:03:00.033Z";
    Instant latestTime = InstantUtil.getEarliest(latestTimeString, time);
    // We change all the time formats back to String for comparison because the default behaviour is
    // for the Instant to truncate
    // all 0's so if millis and seconds are all zeros they end up like
    // 2026-03-25T10:03 instead of 2026-03-25T10:03:00.000Z
    assertEquals(InstantUtil.toMilliString(latestTime), InstantUtil.toMilliString(time));
    Instant earliestTime = InstantUtil.getEarliest(timeString, time);
    assertEquals(InstantUtil.toMilliString(time), InstantUtil.toMilliString(earliestTime));
    assertNotEquals(latestTimeString, InstantUtil.toMilliString(earliestTime));
  }
}
