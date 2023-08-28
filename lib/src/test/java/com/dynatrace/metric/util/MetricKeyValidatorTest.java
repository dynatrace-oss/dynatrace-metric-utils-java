/**
 * Copyright 2023 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.metric.util;

import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetricKeyValidatorTest {
  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideMetricKeys_shouldNotNormalize")
  void testNormalizationRequired_ValidValues(String name, String input) {
    assertFalse(MetricKeyValidator.normalizationRequired(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideMetricKeys_shouldNormalize")
  void testNormalizationRequired_InvalidValues(String name, String input) {
    assertTrue(MetricKeyValidator.normalizationRequired(input));
  }

  private static Stream<Arguments> provideMetricKeys_shouldNotNormalize() {
    return Stream.of(
        Arguments.of("valid base case", "basecase"),
        Arguments.of("valid base case", "just.a.normal.key"),
        Arguments.of("valid leading underscore", "_case"),
        Arguments.of("valid underscore", "case_case"),
        Arguments.of("valid number", "case1"),
        Arguments.of("valid leading uppercase", "Case"),
        Arguments.of("valid all uppercase", "CASE"),
        Arguments.of("valid intermittent uppercase", "someCase"),
        Arguments.of("valid multiple sections", "prefix.case"),
        Arguments.of("valid multiple sections upper", "This.Is.Valid"),
        Arguments.of("valid multiple section leading underscore", "_a.b"),
        Arguments.of("valid leading number second section", "a.0"),
        Arguments.of("valid leading number second section 2", "a.0.c"),
        Arguments.of("valid leading number second section 3", "a.0b.c"),
        Arguments.of("valid trailing hyphen", "dim-"),
        Arguments.of("valid trailing hyphens", "dim---"),
        Arguments.of("valid consecutive leading underscores", "___a"),
        Arguments.of("valid consecutive trailing underscores", "a___"),
        Arguments.of("valid consecutive enclosed underscores", "a___b"),
        Arguments.of("valid mixture dots underscores 2", "_._._.a_._"),
        Arguments.of("valid combined test", "metric.key-number-1.001"),
        Arguments.of("valid example 1", "MyMetric"),
        Arguments.of("valid max length key", TestUtils.repeatStringNTimes("a", 250)));
  }

  private static Stream<Arguments> provideMetricKeys_shouldNormalize() {
    return Stream.of(
        Arguments.of("invalid leading number", "1case"),
        Arguments.of("invalid multiple leading", "!@#case"),
        Arguments.of("invalid multiple trailing", "case!@#"),
        Arguments.of("invalid multiple sections leading number", "0a.b"),
        Arguments.of("invalid leading hyphen", "-dim"),
        Arguments.of("invalid empty", ""),
        Arguments.of("invalid only number", "000"),
        Arguments.of("invalid key first section only number", "0.section"),
        Arguments.of("invalid leading character", "~key"),
        Arguments.of("invalid leading characters", "~0#key"),
        Arguments.of("invalid intermittent character", "some~key"),
        Arguments.of("invalid intermittent characters", "some#~äkey"),
        Arguments.of("invalid two consecutive dots", "a..b"),
        Arguments.of("invalid five consecutive dots", "a.....b"),
        Arguments.of("invalid just a dot", "."),
        Arguments.of("invalid three dots", "..."),
        Arguments.of("invalid leading dot", ".a"),
        Arguments.of("invalid trailing dot", "a.", "a"),
        Arguments.of("invalid enclosing dots", ".a."),
        Arguments.of("invalid trailing invalid chars groups", "a.b$%@.c#@"),
        Arguments.of("invalid mixture dots underscores", "._._._a_._._."),
        Arguments.of("invalid empty section", "an..empty.section"),
        Arguments.of("invalid characters", "a,,,b  c=d\\e\\ =,f"),
        Arguments.of(
            "invalid characters long",
            "a!b\"c#d$e%f&g'h(i)j*k+l,m-n.o/p:q;r<s=t>u?v@w[x]y\\z^0 1_2;3{4|5}6~7"),
        Arguments.of("invalid trailing characters", "a.b.+"),
        Arguments.of("invalid key ends with dot", "a.b."),
        Arguments.of("invalid example 1", "0MyMetric"),
        Arguments.of("invalid example 2", "mÄtric"),
        Arguments.of("invalid example 3", "metriÄ"),
        Arguments.of("invalid example 4", "Ätric"),
        Arguments.of("invalid example 5", "meträääääÖÖÖc"),
        Arguments.of("invalid truncate key too long", TestUtils.repeatStringNTimes("a", 251)));
  }
}
