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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UnitValidatorTest {
  @ParameterizedTest(name = "{index}: {0}, input: \"{1}\"")
  @MethodSource("provideValidUnits")
  public void testValidUnits(String name, String input) {
    assertTrue(UnitValidator.isValidUnit(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: \"{1}\"")
  @MethodSource("provideInvalidUnits")
  void testInvalidUnits(String name, String input) {
    assertFalse(UnitValidator.isValidUnit(input));
  }

  @Test
  void testUnitMaximumStringLength() {
    // valid unit: 63 characters
    String unit63Chars = TestUtils.repeatStringNTimes("a", 63);
    assertTrue(UnitValidator.isValidUnit(unit63Chars));

    // invalid unit: 64 or more characters
    String unit64Chars = TestUtils.repeatStringNTimes("a", 64);
    assertFalse(UnitValidator.isValidUnit(unit64Chars));
  }

  private static Stream<Arguments> provideValidUnits() {
    Stream<Arguments> lowercaseLetters =
        IntStream.rangeClosed(CodePoints.A_LOWERCASE, CodePoints.Z_LOWERCASE)
            .mapToObj(
                cp ->
                    Arguments.of(
                        String.format("valid lowercase %s", TestUtils.codePointToString(cp)),
                        TestUtils.codePointToString(cp)));

    Stream<Arguments> uppercaseLetters =
        IntStream.rangeClosed(CodePoints.A_UPPERCASE, CodePoints.Z_UPPERCASE)
            .mapToObj(
                cp ->
                    Arguments.of(
                        String.format("valid uppercase %s", TestUtils.codePointToString(cp)),
                        TestUtils.codePointToString(cp)));

    Stream<Arguments> digits =
        IntStream.rangeClosed(0, 9)
            .mapToObj(i -> Arguments.of(String.format("valid digit %d", i), String.valueOf(i)));

    Stream<Arguments> specialCases =
        Stream.of(
            Arguments.of("valid open square bracket", "["),
            Arguments.of("valid closed square bracket", "]"),
            Arguments.of("valid text between square brackets", "[abYZ09]"),
            Arguments.of("valid percent sign", "%"),
            Arguments.of("valid forward slash", "/"),
            Arguments.of("valid underscore", "_"),
            Arguments.of("valid all combined", "[ab09YZ%/_]"));

    return Stream.concat(
        lowercaseLetters, Stream.concat(uppercaseLetters, Stream.concat(digits, specialCases)));
  }

  private static Stream<Arguments> provideInvalidUnits() {
    return Stream.of(
        // code point for index 0 is the NUL character.
        Arguments.of("invalid NUL char", TestUtils.codePointToString(0)),
        Arguments.of("invalid Dollar sign", "$"),
        Arguments.of("invalid Euro sign", "\u20AC"),
        Arguments.of("invalid open curly bracket", "{"),
        Arguments.of("invalid closed curly bracket", "}"),
        Arguments.of("invalid text enclosed in curly brackets", "{unit}"),
        // these are all characters that might occur in the metric line itself but not in the unit
        // string.
        Arguments.of("invalid space", " "),
        Arguments.of("invalid dot", "."),
        Arguments.of("invalid comma", ","),
        Arguments.of("invalid escaped space", "\\ "),
        Arguments.of("invalid hash mark", "#"),
        Arguments.of("invalid quotes", "\""),
        Arguments.of("one invalid character makes the whole unit invalid", "spaceIs NotAllowed"),
        Arguments.of("invalid null", null),
        Arguments.of("invalid empty string", ""));
  }
}
