package com.dynatrace.metric.util;

import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StringValueValidatorTest {
  private static final int MAX_TEST_STRING_LENGTH = 255;

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideShouldNotNormalizeStringValues")
  void needsToNormalizeStringValue_ValidValues(String name, String value) {
    assertFalse(
      StringValueValidator.normalizationRequiredStringValue(value, MAX_TEST_STRING_LENGTH));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideShouldNormalizeStringValues")
  void needsToNormalizeStringValue_InvalidValues(String name, String value) {
    assertTrue(
      StringValueValidator.normalizationRequiredStringValue(value, MAX_TEST_STRING_LENGTH));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideShouldNotNormalizeQuotedValues")
  void needsToNormalizeQuotedStringValue_ValidValues(String name, String value) {
    assertFalse(StringValueValidator.normalizationRequiredQuotedStringValue(value, MAX_TEST_STRING_LENGTH));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideShouldNormalizeQuotedValues")
  void needsToNormalizeQuotedStringValue_InvalidValues(String name, String value) {
    assertTrue(StringValueValidator.normalizationRequiredQuotedStringValue(value, MAX_TEST_STRING_LENGTH));
  }

  private static Stream<Arguments> provideShouldNotNormalizeStringValues() {
    return Stream.of(
      Arguments.of("valid value", "value"),
      Arguments.of("valid with dot", "my.value"),
      Arguments.of("valid with underscore", "my_value"),
      Arguments.of("valid with brackets", "[(my_value)]"),
      Arguments.of("valid uppercase", "VALUE"),
      Arguments.of("valid colon", "a:3"),
      Arguments.of("valid special chars", "~@#ä"),
      Arguments.of("valid with leading single quote", "'something"),
      Arguments.of("valid with trailing single quote", "something'"),
      Arguments.of("valid wrapped in single quotes", "'something'"),
      //     'Ab' in unicode:
      Arguments.of("valid unicode", "\u0034\u0066"),
      //     A umlaut, a with ring, O umlaut, U umlaut, all valid.
      Arguments.of("valid unicode", "\u0132_\u0133_\u0150_\u0156"),
      Arguments.of("valid long value", TestUtils.createStringOfLength(255, false)),
      Arguments.of("Emojis of category UNASSIGNED APM-360016",
        "\uD83C\uDDF0\u0038\uD83C\uDDF8\uD83E\uDDEA\uD83E\uDDB8")
    );
  }

  private static Stream<Arguments> provideShouldNormalizeStringValues() {
    return Stream.of(
      Arguments.of("invalid comma", "a,b"),
      Arguments.of("invalid equals", "a=b"),
      Arguments.of("invalid spaces", "a b"),
      Arguments.of("invalid with leading quote", "\"something"),
      Arguments.of("invalid with trailing quote", "something\""),
      Arguments.of("invalid backslash", "a\\b"),
      Arguments.of("invalid multiple special chars", " ,=\\"),
      Arguments.of("invalid key-value pair", "key=\"value\""),
      Arguments.of("invalid with quoted and extra", "\"mylist\":[a,b,c]"),
      Arguments.of("invalid only comma", ","),
      //     \u0000 NUL character, \u0007 bell character
      Arguments.of("invalid unicode", "\u0000a\u0007"),
      Arguments.of("invalid unicode space", "a\u0001b"),
      Arguments.of("invalid leading unicode NUL", "\u0000a"),
      Arguments.of("invalid trailing unicode NUL", "a\u0000"),
      Arguments.of("invalid only unicode", "\u0000\u0000"),
      Arguments.of("invalid consecutive leading unicode", "\u0000\u0000\u0000a"),
      Arguments.of("invalid consecutive trailing unicode", "a\u0000\u0000\u0000"),
      Arguments.of("invalid enclosed unicode NUL", "a\u0000b"),
      Arguments.of("invalid consecutive enclosed unicode NUL", "a\u0000\u0007\u0000b"),
      Arguments.of("invalid unicode in quoted string", "a\u0000\u0007\u0000b"),
      Arguments.of("invalid example 1", "value\u0000"),
      Arguments.of("invalid example 2", "value\u0000end"),
      Arguments.of("invalid example 3", "\u0000end"),
      Arguments.of("invalid value too long", TestUtils.createStringOfLength(256, false)),
      Arguments.of("invalid leading =", String.format("%s=", TestUtils.createStringOfLength(254, false))
      )
    );
  }

  private static Stream<Arguments> provideShouldNotNormalizeQuotedValues() {
    return Stream.of(
      Arguments.of("valid value", "\"value\""),
      Arguments.of("valid with dot", "\"my.value\""),
      Arguments.of("valid with underscore", "\"my_value\""),
      Arguments.of("valid with brackets", "\"[(my_value)]\""),
      Arguments.of("valid uppercase", "\"VALUE\""),
      Arguments.of("valid colon", "\"a:3\""),
      Arguments.of("valid comma", "\"a,b\""),
      Arguments.of("valid equals", "\"a=b\""),
      Arguments.of("valid spaces", "\"a b\""),
      Arguments.of("valid multiple special chars", "\",= (*&^%$~@#ä\""),
      Arguments.of("valid with quoted and extra", "\"mylist:[a,b,c]\""),
      Arguments.of("valid only comma", "\",\""),
      Arguments.of("valid unicode", "\"\u0034\u0066\""),
      Arguments.of("valid unicode", "\"\u0132_\u0133_\u0150_\u0156\""),
      Arguments.of("valid long value", TestUtils.createStringOfLength(253, true)),
      Arguments.of("Emojis of category UNASSIGNED APM-360016",
        "\"\uD83C\uDDF0\u0038\uD83C\uDDF8\uD83E\uDDEA\uD83E\uDDB8\"")
    );
  }

  private static Stream<Arguments> provideShouldNormalizeQuotedValues() {
    return Stream.of(
      Arguments.of("invalid backslash", "\"a\\b\""),
      Arguments.of("invalid key-value pair", "\"key=\"value\"\""),
      Arguments.of("invalid with leading quote", "\"\"something\""),
      Arguments.of("invalid with trailing quote", "\"something\"\""),
      //     \u0000 NUL character, \u0007 bell character
      Arguments.of("invalid unicode", "\"\u0000a\u0007\""),
      Arguments.of("invalid unicode space", "\"a\u0001b\""),
      Arguments.of("invalid leading unicode NUL", "\"\u0000a\""),
      Arguments.of("invalid trailing unicode NUL", "\"a\u0000\""),
      Arguments.of("invalid only unicode", "\"\u0000\u0000\""),
      Arguments.of("invalid consecutive leading unicode", "\"\u0000\u0000\u0000a\""),
      Arguments.of("invalid consecutive trailing unicode", "\"a\u0000\u0000\u0000\""),
      Arguments.of("invalid enclosed unicode NUL", "\"a\u0000b\""),
      Arguments.of("invalid consecutive enclosed unicode NUL", "\"a\u0000\u0007\u0000b\""),
      Arguments.of("invalid example 1", "\"value\u0000\""),
      Arguments.of("invalid example 2", "\"value\u0000end\""),
      Arguments.of("invalid example 3", "\"\u0000end\""),
      // 255 chars + 2 quotes
      Arguments.of("invalid value too long", TestUtils.createStringOfLength(255, true)),
      Arguments.of("invalid leading backslash", String.format("\"%s\\\"", TestUtils.createStringOfLength(252, false)))
    );
  }
}