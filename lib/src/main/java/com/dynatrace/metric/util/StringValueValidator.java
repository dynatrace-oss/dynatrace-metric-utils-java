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

/** Offers validation methods for metric-line specific strings */
final class StringValueValidator {
  private StringValueValidator() {}

  /**
   * Checks if the given string is either empty or null.
   *
   * @param value The string value, which should be checked
   * @return True, if the value is empty or null, otherwise false.
   */
  static boolean isNullOrEmpty(final String value) {
    return value == null || value.isEmpty();
  }

  /**
   * Checks if the codepoint must be escaped.
   *
   * @param codePoint The codepoint.
   * @return True if the codepoint falls into the list of chars to escape, false otherwise.
   */
  static boolean shouldEscapeString(int codePoint) {
    return codePoint == CodePoints.COMMA
        || codePoint == CodePoints.EQUALS
        || codePoint == CodePoints.BLANK
        || codePoint == CodePoints.BACKSLASH
        || codePoint == CodePoints.QUOTE;
  }

  /**
   * Checks if the codepoint of a quoted dimension value should be escaped.
   *
   * @param codePoint The codepoint.
   * @return True if the codepoint falls into the list of chars to escape, false otherwise.
   */
  static boolean shouldEscapeQuotedString(int codePoint) {
    return codePoint == CodePoints.QUOTE || codePoint == CodePoints.BACKSLASH;
  }

  /**
   * Checks if the string is composed of only empty quotes.
   *
   * @param value The string value.
   * @return True if the string value is two quotes (""""), false otherwise.
   */
  static boolean isEmptyQuoted(String value) {
    return "\"\"".equals(value);
  }

  /**
   * Checks if the codepoint is an invalid codepoint (control chars)
   *
   * @param codePoint The codepoint.
   * @return True if the codepoint falls into the list of invalid chars for dimension values, false
   *     otherwise.
   */
  static boolean isInvalidCodePoint(int codePoint) {
    switch (Character.getType(codePoint)) {
      case Character.UNASSIGNED:
        // support all emojis of unicode range "Supplemental Symbols and Pictographs"
        // see https://www.unicode.org/charts/PDF/Unicode-14.0/U140-1F900.pdf
        if (codePoint >= CodePoints.UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_START
            && codePoint <= CodePoints.UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_END) {
          return false;
        }
        // else fallthrough
      case Character.CONTROL:
      case Character.FORMAT:
      case Character.PRIVATE_USE:
      case Character.SURROGATE:
      case Character.LINE_SEPARATOR:
      case Character.PARAGRAPH_SEPARATOR:
        return true;
      default:
        return false;
    }
  }

  /**
   * Calculates if by adding the next character, the string length is still less or equal than the
   * limit. It considers the next character + any necessary escaping character or final quote in
   * case of quoted strings.
   *
   * @param currentValueLength The current length of the string.
   * @param codePoint The next character to be added to the StringBuilder.
   * @param isQuoted Indicates whether the string is a quoted value. This causes the calculation to
   *     consider the length of the quote character.
   * @param includeEscapeChar Indicates whether the char to be added should be escaped. This causes
   *     the calculation to consider the length of the backslash character.
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions
   * @return True if the next character fits and can be safely added to the StringBuilder and false
   *     if not.
   */
  static boolean canAppendToValue(
      int currentValueLength,
      int codePoint,
      boolean isQuoted,
      boolean includeEscapeChar,
      int maxDimensionValueLength) {
    int sizeToAdd = currentValueLength + Character.charCount(codePoint);

    if (isQuoted) {
      sizeToAdd += Character.charCount(CodePoints.QUOTE);
    }

    if (includeEscapeChar) {
      sizeToAdd += Character.charCount(CodePoints.BACKSLASH);
    }
    return sizeToAdd <= maxDimensionValueLength;
  }

  /**
   * Iterates through the string dimension value once to find out if it needs to be normalized.
   *
   * @param value The dimension string value
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions
   * @return True if it needs to be normalized (truncated, replaced or escaped), false otherwise.
   */
  static boolean normalizationRequiredUnqoutedStringValue(
      String value, int maxDimensionValueLength) {
    final int length = value.length();

    if (length > maxDimensionValueLength) {
      return true;
    }

    for (int offset = 0; offset < length; ) {
      final int codePoint = value.codePointAt(offset);

      if (isInvalidCodePoint(codePoint) || shouldEscapeString(codePoint)) {
        return true;
      }

      offset += Character.charCount(codePoint);
    }
    return false;
  }

  /**
   * Iterates through the quoted string dimension value once to find out if it needs to be
   * normalized.
   *
   * @param value The quoted dimension string value
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions
   * @return True if it needs to be normalized (truncated, replaced or escaped), false otherwise.
   */
  static boolean normalizationRequiredQuotedStringValue(String value, int maxDimensionValueLength) {
    final int length = value.length();

    if (length > maxDimensionValueLength) {
      return true;
    }

    final int quoteSize = Character.charCount(CodePoints.QUOTE);
    final int contentLength = length - quoteSize;

    // start from the first quote (thus skipping it) and stops before the last one
    for (int offset = quoteSize; offset < contentLength; ) {
      final int codePoint = value.codePointAt(offset);

      if (isInvalidCodePoint(codePoint) || shouldEscapeQuotedString(codePoint)) {
        return true;
      }

      offset += Character.charCount(codePoint);
    }
    return false;
  }

  enum State {
    START,
    QUOTED_STRING_INTERMEDIATE,
    QUOTED_STRING,
  }
}
