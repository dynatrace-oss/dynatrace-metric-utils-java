package com.dynatrace.metric.util;

import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StringValueNormalizerTest {

  private static final int MAX_TEST_STRING_LENGTH = 255;

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideUnquotedStringValues")
  void testNormalizeUnquotedStringValues(String name, String input, String expected) {
    assertEquals(expected, StringValueNormalizer.normalizeStringValue(input, MAX_TEST_STRING_LENGTH));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideQuotedStringValues")
  void testNormalizeQuotedStringValues(String name, String input, String expected) {
    assertEquals(expected, StringValueNormalizer.normalizeStringValue(input, MAX_TEST_STRING_LENGTH));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}")
  @MethodSource("provideToEscapeValues")
  void testEscapingWorks(String name, String input, String expected) {
    assertEquals(expected, StringValueNormalizer.normalizeStringValue(input, MAX_TEST_STRING_LENGTH));
  }

  private static Stream<Arguments> provideUnquotedStringValues() {
    return Stream.of(
      Arguments.of("valid value", "value", "value"),
      Arguments.of("valid uppercase", "VALUE", "VALUE"),
      Arguments.of("valid colon", "a:3", "a:3"),
      Arguments.of("valid special chars", "~@#ä", "~@#ä"),
      Arguments.of("valid comma", "a,b", "a\\,b"),
      Arguments.of("valid equals", "a=b", "a\\=b"),
      Arguments.of("valid spaces", "a b", "a\\ b"),
      Arguments.of("valid backslash", "a\\b", "a\\\\b"), // user wants value of a\b, so we escape it to a\\b
      Arguments.of("valid multiple special chars", " ,=\\", "\\ \\,\\=\\\\"),
      Arguments.of("valid key-value pair", "key=\"value\"", "key\\=\\\"value\\\""),
      Arguments.of("valid with leading quote", "\"something", "\\\"something"),
      Arguments.of("valid with trailing quote", "something\"", "something\\\""),
      Arguments.of("valid with leading single quote", "'something", "'something"),
      Arguments.of("valid with trailing single quote", "something'", "something'"),
      Arguments.of("valid wrapped in single quotes", "'something'", "'something'"),
      Arguments.of("valid with quoted and extra", "\"mylist\":[a,b,c]", "\\\"mylist\\\":[a\\,b\\,c]"),
      //     'Ab' in unicode:
      Arguments.of("valid unicode", "\u0034\u0066", "\u0034\u0066"),
      //     A umlaut, a with ring, O umlaut, U umlaut, all valid.
      Arguments.of("valid unicode", "\u0132_\u0133_\u0150_\u0156", "\u0132_\u0133_\u0150_\u0156"),
      Arguments.of("invalid empty", "", null),
      Arguments.of("invalid null", null, null),
      Arguments.of("invalid only comma", ",", "\\,"),
      //     \u0000 NUL character, \u0007 bell character
      Arguments.of("invalid unicode", "\u0000a\u0007", "_a_"),
      Arguments.of("invalid unicode space", "a\u0001b", "a_b"),
      Arguments.of("invalid leading unicode NUL", "\u0000a", "_a"),
      Arguments.of("invalid trailing unicode NUL", "a\u0000", "a_"),
      Arguments.of("invalid only unicode", "\u0000\u0000", "_"),
      Arguments.of("invalid consecutive leading unicode", "\u0000\u0000\u0000a", "_a"),
      Arguments.of("invalid consecutive trailing unicode", "a\u0000\u0000\u0000", "a_"),
      Arguments.of("invalid enclosed unicode NUL", "a\u0000b", "a_b"),
      Arguments.of("invalid consecutive enclosed unicode NUL", "a\u0000\u0007\u0000b", "a_b"),
      Arguments.of("invalid unicode in quoted string", "a\u0000\u0007\u0000b", "a_b"),
      Arguments.of("invalid example 1", "value\u0000", "value_"),
      Arguments.of("invalid example 2", "value\u0000end", "value_end"),
      Arguments.of("invalid example 3", "\u0000end", "_end"),
      Arguments.of(
        "valid long value",
        TestUtils.createStringOfLength(255, false),
        TestUtils.createStringOfLength(255, false)
      ),
      Arguments.of(
        "invalid truncate value too long",
        TestUtils.createStringOfLength(256, false),
        TestUtils.createStringOfLength(255, false)
      ),
      // we have a = at the 255th pos. Escaping it will cause going overboard - 256. It should not append it
      Arguments.of(
        "invalid truncate at 254th",
        String.format("%s=", TestUtils.createStringOfLength(254, false)),
        String.format("%s", TestUtils.createStringOfLength(254, false))
      )
    );
  }

  private static Stream<Arguments> provideQuotedStringValues() {
    return Stream.of(
      Arguments.of("valid value", "\"value\"", "\"value\""),
      Arguments.of("valid uppercase", "\"VALUE\"", "\"VALUE\""),
      Arguments.of("valid colon", "\"a:3\"", "\"a:3\""),
      Arguments.of("valid special chars", "\"~@#ä\"", "\"~@#ä\""),
      Arguments.of("valid comma", "\"a,b\"", "\"a,b\""),
      Arguments.of("valid equals", "\"a=b\"", "\"a=b\""),
      Arguments.of("valid spaces", "\"a b\"", "\"a b\""),
      // user wants value of "a\b" so we escape it to "a\\b"
      Arguments.of("valid backslash", "\"a\\b\"", "\"a\\\\b\""),
      // user wants value of " ,=\" so we escape it to " ,=\\"
      Arguments.of("valid multiple special chars", "\" ,=\\\"", "\" ,=\\\\\""),
      // user wants value of "key="value"" so we escape it to "key=\"value\""
      Arguments.of("valid key-value pair", "\"key=\"value\"\"", "\"key=\\\"value\\\"\""),
      Arguments.of("valid with leading quote", "\"\"something\"", "\"\\\"something\""),
      Arguments.of("valid with trailing quote", "\"something\"\"", "\"something\\\"\""),
      Arguments.of("valid with quoted and extra", "\"mylist:[a,b,c]\"", "\"mylist:[a,b,c]\""),
      Arguments.of("valid only comma", "\",\"", "\",\""),
      //     'Ab' in unicode:
      Arguments.of("valid unicode", "\"\u0034\u0066\"", "\"\u0034\u0066\""),
      //     A umlaut, a with ring, O umlaut, U umlaut, all valid.
      Arguments.of("valid unicode", "\"\u0132_\u0133_\u0150_\u0156\"", "\"\u0132_\u0133_\u0150_\u0156\""),
      Arguments.of("invalid empty", "\"\"", null),
      Arguments.of("invalid null", null, null),
      //     \u0000 NUL character, \u0007 bell character
      Arguments.of("invalid unicode", "\"\u0000a\u0007\"", "\"_a_\""),
      Arguments.of("invalid unicode space", "\"a\u0001b\"", "\"a_b\""),
      Arguments.of("invalid leading unicode NUL", "\"\u0000a\"", "\"_a\""),
      Arguments.of("invalid trailing unicode NUL", "\"a\u0000\"", "\"a_\""),
      Arguments.of("invalid only unicode", "\"\u0000\u0000\"", "\"_\""),
      Arguments.of("invalid consecutive leading unicode", "\"\u0000\u0000\u0000a\"", "\"_a\""),
      Arguments.of("invalid consecutive trailing unicode", "\"a\u0000\u0000\u0000\"", "\"a_\""),
      Arguments.of("invalid enclosed unicode NUL", "\"a\u0000b\"", "\"a_b\""),
      Arguments.of("invalid consecutive enclosed unicode NUL", "\"a\u0000\u0007\u0000b\"", "\"a_b\""),
      Arguments.of("invalid example 1", "\"value\u0000\"", "\"value_\""),
      Arguments.of("invalid example 2", "\"value\u0000end\"", "\"value_end\""),
      Arguments.of("invalid example 3", "\"\u0000end\"", "\"_end\""),
      Arguments.of(
        "invalid truncate value too long",
        TestUtils.createStringOfLength(255, true), // total of 257 with quotes
        TestUtils.createStringOfLength(253, true) // total of 255 with quotes
      ),
      // we have a \ at the 254th pos. Escaping it will cause going overboard - 256. It should truncate with a "
      Arguments.of(
        "invalid truncate at 254th",
        String.format("\"%s\\\"", TestUtils.createStringOfLength(252, false)),
        String.format("\"%s\"", TestUtils.createStringOfLength(252, false))
      ),
      // we have 254 "a", so adding the last will cause going overboard - 256. It should truncate with a "
      Arguments.of(
        "invalid truncate at 253th",
        String.format("\"%s\"", TestUtils.createStringOfLength(254, false)),
        String.format("\"%s\"", TestUtils.createStringOfLength(253, false))
      )
    );
  }

  private static Stream<Arguments> provideToEscapeValues() {
    return Stream.of(
      Arguments.of("valid comma", "a,b", "a\\,b"),
      Arguments.of("valid equals", "a=b", "a\\=b"),
      Arguments.of("valid spaces", "a b", "a\\ b"),
      Arguments.of("valid backslash", "a\\b", "a\\\\b"), // user wants value of a\b, so we escape it to a\\b
      Arguments.of("valid with leading quote", "\"something", "\\\"something"),
      // user wants value of "a\b" so we escape it to "a\\b"
      Arguments.of("valid quoted string with backslash", "\"a\\b\"", "\"a\\\\b\""),
      // user wants value of " ,=\" so we escape it to " ,=\\"
      Arguments.of("valid quoted string with multiple special chars", "\" ,=\\\"", "\" ,=\\\\\"")
    );
  }

}