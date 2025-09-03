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

import com.dynatrace.metric.util.MetricLineConstants.ValidationMessages;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Offers normalization methods for metric key, dimension key and dimension value
 */
final class Normalizer {


  private Normalizer() {
  }

  /**
   * Converts the double value to a string according their pattern. Values with no floating point
   * are serialized as a long value, whereas values with floating point are serialized as a double
   * value.
   *
   * @param value The double value which should be converted.
   * @return The double value as a string.
   */
  static String doubleToString(final double value) {
    if (value == (long) value) {
      return Long.toString((long) value);
    }

    return Double.toString(value);
  }

  /**
   * Applies normalization to the provided metric key.
   *
   * @param key The metric key to normalize
   * @return The {@link NormalizationResult result}, containing the potentially normalized metric
   * key along with any error or warnings encountered.
   */
  static NormalizationResult normalizeMetricKey(String key) {
    if (StringValueValidator.isNullOrEmpty(key)) {
      return NormalizationResult.newInvalid();
    }

    final int length = key.length();
    boolean needsToTruncate = length > MetricLineConstants.Limits.MAX_METRIC_KEY_LENGTH;
    final int effectiveLength =
      needsToTruncate ? MetricLineConstants.Limits.MAX_METRIC_KEY_LENGTH : length;
    boolean previousCodePointSanitized = false;
    int invalidCharsEncountered = 0;

    StringBuilder sb = new StringBuilder(effectiveLength);

    MetricKeyValidator.State state = MetricKeyValidator.State.START;
    for (int offset = 0; offset < effectiveLength; ) {
      final int codePoint = key.codePointAt(offset);
      switch (state) {
        case START:
          // empty first section -> invalid metric key
          if (MetricKeyValidator.isSectionSeparator(codePoint)) {
            if (offset == 0) {
              // empty first section
              return NormalizationResult.newInvalid();
            }
          }

          if (MetricKeyValidator.isValidFirstSectionStartCharacter(codePoint)) {
            sb.appendCodePoint(codePoint);
          } else {
            sb.appendCodePoint(CodePoints.UNDERSCORE);
            previousCodePointSanitized = true;
            invalidCharsEncountered++;
          }

          state = MetricKeyValidator.State.SECTION;
          break;
        case CONSECUTIVE_SECTION_START:
          // skip empty sections
          if (MetricKeyValidator.isSectionSeparator(codePoint)) {
            invalidCharsEncountered++;
            break;
          }

          sb.appendCodePoint(CodePoints.DOT);
          if (MetricKeyValidator.isValidConsecutiveSectionStartCharacter(codePoint)) {
            sb.appendCodePoint(codePoint);
            previousCodePointSanitized = false;
          } else {
            previousCodePointSanitized = true;
            invalidCharsEncountered++;
            sb.appendCodePoint(CodePoints.UNDERSCORE);
          }

          state = MetricKeyValidator.State.SECTION;
          break;
        case SECTION:
          if (MetricKeyValidator.isSectionSeparator(codePoint)) {
            // if we have a trailing dot ignore it
            if (offset + Character.charCount(codePoint) == effectiveLength) {
              invalidCharsEncountered++;
            } else {
              previousCodePointSanitized = false;
              state = MetricKeyValidator.State.CONSECUTIVE_SECTION_START;
            }

            break;
          }

          if (MetricKeyValidator.isValidSection(codePoint)) {
            sb.appendCodePoint(codePoint);
            previousCodePointSanitized = false;
          } else {
            if (!previousCodePointSanitized) {
              sb.appendCodePoint(CodePoints.UNDERSCORE);
              previousCodePointSanitized = true;
            }

            invalidCharsEncountered++;
          }
          break;
      }

      offset += Character.charCount(codePoint);
    }

    String normalizedMetricKey = sb.toString();
    if (invalidCharsEncountered > 0 || needsToTruncate) {
      return NormalizationResult.newWarning(
        normalizedMetricKey,
        () ->
          String.format(
            ValidationMessages.METRIC_KEY_NORMALIZED_MESSAGE, key, normalizedMetricKey));
    }

    return NormalizationResult.newValid(normalizedMetricKey);
  }

  /**
   * Applies normalization to the provided dimension key.
   *
   * @param key The dimension key to normalize.
   * @return The {@link NormalizationResult result}, containing the potentially normalized dimension
   * key along with any error or warnings encountered.
   */
  static NormalizationResult normalizeDimensionKey(String key) {
    if (StringValueValidator.isNullOrEmpty(key)) {
      return NormalizationResult.newValid(null);
    }

    final int length = key.length();
    boolean needsToTruncate = length > MetricLineConstants.Limits.MAX_DIMENSION_KEY_LENGTH;
    final int effectiveLength =
      needsToTruncate ? MetricLineConstants.Limits.MAX_DIMENSION_KEY_LENGTH : length;
    boolean previousCodePointSanitized = false;
    int invalidCharsEncountered = 0;

    StringBuilder sb = new StringBuilder(effectiveLength);

    DimensionKeyValidator.State state = DimensionKeyValidator.State.START;
    for (int offset = 0; offset < effectiveLength; ) {
      final int codePoint = Character.toLowerCase(key.codePointAt(offset));
      switch (state) {
        case START:
          // to ignore trailing or leading dots
          if (DimensionKeyValidator.isSectionSeparator(codePoint)) {
            invalidCharsEncountered++;
            break;
          }

          // to only create non-empty sections
          if (sb.length() != 0) {
            sb.appendCodePoint(CodePoints.DOT);
          }

          if (DimensionKeyValidator.isValidFirstSectionStartCharacter(codePoint)) {
            sb.appendCodePoint(codePoint);
            previousCodePointSanitized = false;
          } else {
            sb.appendCodePoint(CodePoints.UNDERSCORE);
            previousCodePointSanitized = true;
            invalidCharsEncountered++;
          }
          state = DimensionKeyValidator.State.SECTION;
          break;
        case SECTION:
          if (DimensionKeyValidator.isSectionSeparator(codePoint)) {
            // if the dot is the last char, we need to normalize it
            if (offset + Character.charCount(codePoint) == effectiveLength) {
              invalidCharsEncountered++;
            } else {
              previousCodePointSanitized = false;
              state = DimensionKeyValidator.State.START;
            }
            break;
          }

          if (DimensionKeyValidator.isValidSection(codePoint)) {
            sb.appendCodePoint(codePoint);
            previousCodePointSanitized = false;
          } else {
            if (!previousCodePointSanitized) {
              sb.appendCodePoint(CodePoints.UNDERSCORE);
              previousCodePointSanitized = true;
            }
            invalidCharsEncountered++;
          }
          break;
      }
      offset += Character.charCount(codePoint);
    }

    if (sb.length() == 0) {
      return NormalizationResult.newInvalid(
        () ->
          String.format(
            ValidationMessages.DIMENSION_KEY_NORMALIZED_MESSAGE,
            key,
            CodePoints.EMPTY_STRING));
    }

    String normalizedDimKey = sb.toString();
    if (invalidCharsEncountered > 0 || needsToTruncate) {
      return NormalizationResult.newWarning(
        normalizedDimKey,
        () ->
          String.format(
            ValidationMessages.DIMENSION_KEY_NORMALIZED_MESSAGE, key, normalizedDimKey));
    }

    return NormalizationResult.newValid(normalizedDimKey);
  }

  /**
   * Applies normalization to the provided dimension value.
   *
   * @param value                   The dimension value to normalize.
   * @param maxDimensionValueLength The maximum value for dimension keys.
   * @return The {@link NormalizationResult result}, containing the potentially normalized dimension
   * value along with any error or warnings encountered.
   */
  static NormalizationResult normalizeDimensionValue(String value, int maxDimensionValueLength) {
    if (StringValueValidator.isNullOrEmpty(value)) {
      return NormalizationResult.newValid(CodePoints.EMPTY_STRING);
    }

    boolean isQuoted =
      value.startsWith(CodePoints.QUOTATION_MARK) && value.endsWith(CodePoints.QUOTATION_MARK);
    if (isQuoted) {
      return normalizeQuotedDimValue(value, maxDimensionValueLength);
    }
    return normalizeUnquotedStringDimValue(value, maxDimensionValueLength);
  }

  static NormalizationResult normalizeMetadataString(String value, int maxLength) {
    if (StringValueValidator.isNullOrEmpty(value)) {
      return NormalizationResult.newValid(CodePoints.EMPTY_STRING);
    }

    // in a quoted string, quotes do not count towards the string character limit
    boolean isQuoted =
      value.startsWith(CodePoints.QUOTATION_MARK) && value.endsWith(CodePoints.QUOTATION_MARK);

    int numValidBytes = 0;
    boolean needsNormalization = false;
    boolean needsEscaping = false;

    // if quoted, start at index 1 (the one after the quote)
    int start = isQuoted ? 1 : 0;
    // if quoted, stop at length - 1 (the last byte before the quote)
    int end = isQuoted ? value.length() - 1 : value.length();

    for (int i = start; i < end; ) {
      final int codePoint = value.codePointAt(i);
      final int codePointLength = Character.charCount(codePoint);

      if (codePointNeedsEscaping(codePoint)) {
        needsEscaping = true;
      }

      // newline is a control character, which is disallowed, but newline is the exception
      // descriptions can have newlines in them
      if (isInvalidCharForMetadata(codePoint)) {
        // if there are control characters, note that normalization is necessary but don't break yet
        // this way we know how many characters there will be in the output
        needsNormalization = true;
      } else {
        // if the char is valid, count up
        numValidBytes += codePointLength;
        // if the maxLength is reached, break.
        if (numValidBytes > maxLength) {
          needsNormalization = true;
          break;
        }
      }
      i += codePointLength;
    }

    // return early if no normalization is needed.
    if (!needsNormalization) {
      // if the string is already quoted or doesn't need escaping, return as is
      if (isQuoted || !needsEscaping) {
        return NormalizationResult.newValid(value);
      } else {
        // not quoted, needs escaping, but no normalization (e.g., it contains a '=' character).
        // turn the string into a quoted string, where the '=' sign does not need escaping.
        return NormalizationResult.newValid("\"" + value + "\"");
      }
    }


    // do normalization
    StringBuilder builder = new StringBuilder(numValidBytes + 2);
    builder.append(CodePoints.QUOTATION_MARK);
    Supplier<String> warningMessageSupplier = null;

    // dont need to escape anything since were working in a quoted string.
    for (int i = start; i < end; ) {
      final int codePoint = value.codePointAt(i);
      final int codePointLength = Character.charCount(codePoint);

      // special handling: only newlines are considered, since the resulting string will be quoted either way.
      if (codePoint == CodePoints.NEWLINE) {
        if (i + CodePoints.ESCAPED_NEWLINE.length() > maxLength) {
          break;
        }
        builder.append(CodePoints.ESCAPED_NEWLINE);
      } else if (codePoint == CodePoints.QUOTE) {
        if (i + CodePoints.ESCAPED_QUOTES.length() > maxLength) {
          break;
        }
        builder.append(CodePoints.ESCAPED_QUOTES);
      } else if (isInvalidCharForMetadata(codePoint)) {
        // skip current code point if it's invalid
        i += codePointLength;
        continue;
      } else {
        if (i + codePointLength > maxLength) {
          break;
        }
        builder.appendCodePoint(codePoint);
      }

      i += codePointLength;
    }
    builder.append(CodePoints.QUOTATION_MARK);

    String normalized = builder.toString();
    if (normalized.length() == 2) {
      return NormalizationResult.newInvalid(() -> "no valid characters after normalization (input: " + value + ")");
    } else return NormalizationResult.newWarning(normalized,
      () -> String.format(ValidationMessages.METADATA_VALUE_NORMALIZED_MESSAGE, value, normalized));
  }

  private static boolean codePointNeedsEscaping(int codePoint) {
    return codePoint == "\n".codePointAt(0) ||
           codePoint == " ".codePointAt(0) ||
           codePoint == ",".codePointAt(0) ||
           codePoint == "=".codePointAt(0) ||
           codePoint == "\"".codePointAt(0) ||
           codePoint == "\\".codePointAt(0);
  }

  private static boolean isInvalidCharForMetadata(int codePoint) {
    int type = Character.getType(codePoint);

    // unassigned characters outside the range of Unicode 10.0 "Supplemental Symbols and Pictographs" are not allowed.
    if (type == Character.UNASSIGNED) {
      // Unicode version 10 allowed Supplemental Symbols and pictographs
      // https://www.unicode.org/charts/PDF/Unicode-10.0/U100-1F900.pdf
      return codePoint <= CodePoints.UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_START ||
             codePoint >= CodePoints.UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_END;
    }

    return
      type == Character.CONTROL ||
      type == Character.PRIVATE_USE ||
      type == Character.SURROGATE ||
      type == Character.LINE_SEPARATOR ||
      type == Character.PARAGRAPH_SEPARATOR ||
      codePoint == "\"".codePointAt(0);
  }

  /**
   * Applies normalization to an unquoted string dimension value.
   *
   * @param value                   The unquoted dimension value.
   * @param maxDimensionValueLength The maximum value to compare to. E.g. The maximum value for
   *                                dimensions.
   * @return The {@link NormalizationResult result}, containing the potentially normalized dimension
   * value along with any error or warnings encountered.
   */
  private static NormalizationResult normalizeUnquotedStringDimValue(
    String value, int maxDimensionValueLength) {
    if (!StringValueValidator.normalizationRequiredUnqoutedStringValue(
      value, maxDimensionValueLength)) {
      return NormalizationResult.newValid(value);
    }

    StringBuilder sb = new StringBuilder();
    final int length = value.length();
    boolean previousCodePointSanitized = false;
    int invalidCharsEncountered = 0;
    boolean wasTruncated = false;

    for (int offset = 0; offset < length; ) {
      final int codePoint = value.codePointAt(offset);

      if (StringValueValidator.isInvalidCodePoint(codePoint)) {
        if (previousCodePointSanitized) {
          offset += Character.charCount(codePoint);
          continue;
        }
        if (!StringValueValidator.canAppendToValue(
          sb.length(), CodePoints.UNDERSCORE, false, false, maxDimensionValueLength)) {
          wasTruncated = true;
          break;
        }
        sb.appendCodePoint(CodePoints.UNDERSCORE);
        previousCodePointSanitized = true;
        invalidCharsEncountered++;
      } else {
        boolean shouldEscape = StringValueValidator.shouldEscapeString(codePoint);
        boolean canAppend =
          StringValueValidator.canAppendToValue(
            sb.length(), codePoint, false, shouldEscape, maxDimensionValueLength);

        if (!canAppend) {
          wasTruncated = true;
          break;
        }

        if (shouldEscape) {
          sb.appendCodePoint(CodePoints.BACKSLASH).appendCodePoint(codePoint);
        } else {
          sb.appendCodePoint(codePoint);
        }
        previousCodePointSanitized = false;
      }
      offset += Character.charCount(codePoint);
    }

    String normalizedDimValue = sb.toString();
    if (invalidCharsEncountered > 0 || wasTruncated) {
      return NormalizationResult.newWarning(
        normalizedDimValue,
        () ->
          String.format(
            ValidationMessages.DIMENSION_VALUE_NORMALIZED_MESSAGE,
            value,
            normalizedDimValue));
    }

    return NormalizationResult.newValid(normalizedDimValue);
  }

  /**
   * Applies normalization to the quoted string dimension value.
   *
   * @param value                   The quoted dimension value.
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *                                dimensions.
   * @return The {@link NormalizationResult result}, containing the potentially normalized dimension
   * value along with any error or warnings encountered.
   */
  private static NormalizationResult normalizeQuotedDimValue(
    String value, int maxDimensionValueLength) {
    if (!StringValueValidator.normalizationRequiredQuotedStringValue(
      value, maxDimensionValueLength)) {
      return NormalizationResult.newValid(value);
    }

    StringBuilder sb = new StringBuilder();
    final int length = value.length();
    boolean previousCodePointSanitized = false;
    int invalidCharsEncountered = 0;
    boolean wasTruncated = false;

    StringValueValidator.State state = StringValueValidator.State.START;

    for (int offset = 0; offset < length; ) {
      final int codePoint = value.codePointAt(offset);
      switch (state) {
        case START:
          sb.appendCodePoint(codePoint);
          state = StringValueValidator.State.QUOTED_STRING_INTERMEDIATE;
          break;
        case QUOTED_STRING_INTERMEDIATE:
          boolean isLastChar = offset + Character.charCount(codePoint) == length;
          // don't escape the quote if it's the last char
          if (codePoint == CodePoints.QUOTE && isLastChar) {
            sb.appendCodePoint(codePoint);
            state = StringValueValidator.State.QUOTED_STRING;
            break;
          }
          if (StringValueValidator.isInvalidCodePoint(codePoint)) {
            if (previousCodePointSanitized) {
              break;
            }
            if (!StringValueValidator.canAppendToValue(
              sb.length(), CodePoints.UNDERSCORE, true, false, maxDimensionValueLength)) {
              wasTruncated = true;
              break;
            }
            sb.appendCodePoint(CodePoints.UNDERSCORE);
            previousCodePointSanitized = true;
            invalidCharsEncountered++;
            break;
          }
          boolean shouldEscape = StringValueValidator.shouldEscapeQuotedString(codePoint);
          boolean canAppend =
            StringValueValidator.canAppendToValue(
              sb.length(), codePoint, true, shouldEscape, maxDimensionValueLength);

          if (!canAppend) {
            wasTruncated = true;
            break;
          }

          if (shouldEscape) {
            sb.appendCodePoint(CodePoints.BACKSLASH).appendCodePoint(codePoint);
          } else {
            sb.appendCodePoint(codePoint);
          }
          previousCodePointSanitized = false;
          break;
      }
      if (wasTruncated) {
        // Truncate the string with the final quote and stop the loop
        sb.appendCodePoint(CodePoints.QUOTE);
        break;
      }
      offset += Character.charCount(codePoint);
    }
    String normalizedDimValue = sb.toString();
    if (invalidCharsEncountered > 0 || wasTruncated) {
      return NormalizationResult.newWarning(
        normalizedDimValue,
        () ->
          String.format(
            ValidationMessages.DIMENSION_VALUE_NORMALIZED_MESSAGE,
            value,
            normalizedDimValue));
    }

    return NormalizationResult.newValid(normalizedDimValue);
  }
}
