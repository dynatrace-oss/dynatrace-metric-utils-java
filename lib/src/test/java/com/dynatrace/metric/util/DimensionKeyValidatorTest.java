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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dynatrace.testutils.TestUtils;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DimensionKeyValidatorTest {

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideDimensionKeys_shouldNotNormalize")
  void testNormalizationRequired_ValidValues(String name, String input) {
    assertFalse(DimensionKeyValidator.normalizationRequired(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideDimensionKeys_shouldNormalize")
  void testNormalizationRequired_InvalidValues(String name, String input) {
    assertTrue(DimensionKeyValidator.normalizationRequired(input));
  }

  private static Stream<Arguments> provideDimensionKeys_shouldNotNormalize() {
    return Stream.of(
        Arguments.of("valid value", "value"),
        Arguments.of("valid leading underscore", "_value"),
        Arguments.of("valid leading 'a'", "avalue"),
        Arguments.of("valid leading 'z'", "zvalue"),
        Arguments.of("valid with dot", "my.value"),
        Arguments.of("valid with colon", "dim:dim", "dim:dim"),
        Arguments.of("valid with underscore", "dim_dim", "dim_dim"),
        Arguments.of("valid with trailing number", "something1"),
        Arguments.of("valid with trailing hyphen", "something-"),
        Arguments.of("valid with trailing colon", "something:"),
        Arguments.of("valid with trailing underscore", "something_"),
        Arguments.of("valid case", "dim"),
        Arguments.of("valid number", "dim1"),
        Arguments.of("valid trailing hyphens", "dim---"),
        Arguments.of("valid trailing underscores", "aaa___"),
        Arguments.of("valid compound key", "dim1.value1"),
        Arguments.of("valid compound leading underscore", "dim._val"),
        Arguments.of("valid compound only underscore", "dim.___"),
        Arguments.of("valid compound long", "dim.dim.dim.dim"),
        Arguments.of("valid colon in compound", "a.b:c.d"),
        Arguments.of("valid combined key", "dim.val:count.val001"),
        Arguments.of("valid long value", TestUtils.createStringOfLength(100, false)));
  }

  private static Stream<Arguments> provideDimensionKeys_shouldNormalize() {
    return Stream.of(
        Arguments.of("invalid unicode", "\u0034"),
        Arguments.of("invalid start with a dot", ".value"),
        Arguments.of("invalid start with a colon", ":value"),
        Arguments.of("invalid start with a minus", "-value"),
        Arguments.of("invalid blank value", " "),
        Arguments.of("invalid uppercase letter in value", "my.Value"),
        Arguments.of("invalid leading uppercase", "Dim"),
        Arguments.of("invalid internal uppercase", "dIm"),
        Arguments.of("invalid trailing uppercase", "diM"),
        Arguments.of("invalid leading umlaut and uppercase", "äABC"),
        Arguments.of("invalid multiple leading", "!@#case"),
        Arguments.of("invalid multiple trailing", "case!@#"),
        Arguments.of("invalid all uppercase", "DIM"),
        Arguments.of("invalid leading hyphen", "-dim"),
        Arguments.of("invalid leading multiple hyphens", "---dim"),
        Arguments.of("invalid leading colon", ":dim"),
        Arguments.of("invalid chars", "~@#ä"),
        Arguments.of("invalid trailing chars", "aaa~@#ä"),
        Arguments.of("invalid only numbers", "000"),
        Arguments.of("invalid compound leading number", "dim.0dim"),
        Arguments.of("invalid compound only number", "dim.000"),
        Arguments.of("invalid compound leading invalid char", "dim.~val"),
        Arguments.of("invalid compound trailing invalid char", "dim.val~~"),
        Arguments.of("invalid compound only invalid char", "dim.~~~"),
        Arguments.of("invalid two dots", "a..b"),
        Arguments.of("invalid five dots", "a.....b"),
        Arguments.of("invalid leading dot", ".a"),
        Arguments.of("invalid trailing dot", "a."),
        Arguments.of("invalid just a dot", "."),
        Arguments.of("invalid trailing dots", "a..."),
        Arguments.of("invalid enclosing dots", ".a."),
        Arguments.of("invalid leading whitespace", "   a"),
        Arguments.of("invalid trailing whitespace", "a   "),
        Arguments.of("invalid internal whitespace", "a b"),
        Arguments.of("invalid internal whitespace", "a    b"),
        Arguments.of("invalid empty", ""),
        Arguments.of("invalid characters", "a,,,b  c=d\\e\\ =,f"),
        Arguments.of(
            "invalid characters long",
            "a!b\"c#d$e%f&g'h(i)j*k+l,m-n.o/p:q;r<s=t>u?v@w[x]y\\z^0 1_2;3{4|5}6~7"),
        Arguments.of("invalid example 1", "Tag"),
        Arguments.of("invalid example 2", "0Tag"),
        Arguments.of("invalid example 3", "tÄg"),
        Arguments.of("invalid example 4", "mytäääg"),
        Arguments.of("invalid example 5", "ääätag"),
        Arguments.of("invalid example 6", "ä_ätag"),
        Arguments.of("invalid example 7", "Bla___"),
        Arguments.of("invalid value too long", TestUtils.createStringOfLength(101, false)));
  }
}
