package com.dynatrace.metric.util;

import com.dynatrace.metric.util.validation.CodePoints;
import com.dynatrace.metric.util.validation.DimensionValueValidator;

/**
 * Utility class containing methods to apply normalization to metric keys and dimensions, according
 * to the specification
 */
public class LineNormalizer {

  private LineNormalizer() {}

  /**
   * Applies normalization to the provided dimension value according to the spec.
   *
   * @param value The dimension value to normalize.
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions.
   * @return The {@link String result}, containing the potentially normalized value along with any
   *     warning encountered.
   */
  public static String normalizeDimensionValue(String value, int maxDimensionValueLength) {
    boolean isQuoted =
        value.startsWith(CodePoints.QUOTATION_MARK) && value.endsWith(CodePoints.QUOTATION_MARK);
    if (isQuoted) {
      return normalizeQuotedDimValue(value, maxDimensionValueLength);
    }
    return normalizeStringDimValue(value, maxDimensionValueLength);
  }

  /**
   * Applies normalization to the string dimension value.
   *
   * @param value The string dimension value.
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions.
   * @return The {@link String result}, containing the potentially normalized value along with any
   *     warning encountered.
   */
  private static String normalizeStringDimValue(String value, int maxDimensionValueLength) {
    if (!DimensionValueValidator.normalizationRequiredStringValue(value, maxDimensionValueLength)) {
      return value;
    }

    StringBuilder sb = new StringBuilder();
    final int length = value.length();
    boolean wasNormalizedBefore = false;

    for (int offset = 0; offset < length; ) {
      final int codePoint = value.codePointAt(offset);

      if (DimensionValueValidator.isInvalidCodePoint(codePoint)) {
        if (wasNormalizedBefore) {
          offset += Character.charCount(codePoint);
          continue;
        }
        if (!DimensionValueValidator.canAppendToValue(
            sb.length(), CodePoints.UNDERSCORE, false, false, maxDimensionValueLength)) {
          break;
        }
        sb.appendCodePoint(CodePoints.UNDERSCORE);
        wasNormalizedBefore = true;
      } else {
        boolean shouldEscape = DimensionValueValidator.shouldEscapeString(codePoint);
        boolean canAppend =
            DimensionValueValidator.canAppendToValue(
                sb.length(), codePoint, false, shouldEscape, maxDimensionValueLength);

        if (!canAppend) {
          break;
        }

        if (shouldEscape) {
          sb.appendCodePoint(CodePoints.BACKSLASH).appendCodePoint(codePoint);
        } else {
          sb.appendCodePoint(codePoint);
        }
        wasNormalizedBefore = false;
      }
      offset += Character.charCount(codePoint);
    }
    return sb.toString();
  }

  /**
   * Applies normalization to the quoted string dimension value.
   *
   * @param value The quoted dimension value.
   * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions.
   * @return The {@link String result}, containing the potentially normalized value along with any
   *     warning encountered.
   */
  private static String normalizeQuotedDimValue(String value, int maxDimensionValueLength) {
    if (!DimensionValueValidator.normalizationRequiredQuotedStringValue(
        value, maxDimensionValueLength)) {
      return value;
    }

    StringBuilder sb = new StringBuilder();
    final int length = value.length();
    boolean wasNormalizedBefore = false;
    boolean wasTruncated = false;

    DimensionValueValidator.State state = DimensionValueValidator.State.START;

    for (int offset = 0; offset < length; ) {
      final int codePoint = value.codePointAt(offset);
      switch (state) {
        case START:
          sb.appendCodePoint(codePoint);
          state = DimensionValueValidator.State.QUOTED_STRING_INTERMEDIATE;
          break;
        case QUOTED_STRING_INTERMEDIATE:
          boolean isLastChar = offset + Character.charCount(codePoint) == length;
          // don't escape the quote if it's the last char
          if (codePoint == CodePoints.QUOTE && isLastChar) {
            sb.appendCodePoint(codePoint);
            state = DimensionValueValidator.State.QUOTED_STRING;
            break;
          }
          if (DimensionValueValidator.isInvalidCodePoint(codePoint)) {
            if (wasNormalizedBefore) {
              break;
            }
            if (!DimensionValueValidator.canAppendToValue(
                sb.length(), CodePoints.UNDERSCORE, true, false, maxDimensionValueLength)) {
              wasTruncated = true;
              break;
            }
            sb.appendCodePoint(CodePoints.UNDERSCORE);
            wasNormalizedBefore = true;
            break;
          }
          boolean shouldEscape = DimensionValueValidator.shouldEscapeQuotedString(codePoint);
          boolean canAppend =
              DimensionValueValidator.canAppendToValue(
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
          wasNormalizedBefore = false;
          break;
      }
      if (wasTruncated) {
        // Truncate the string with the final quote and stop the loop
        sb.appendCodePoint(CodePoints.QUOTE);
        break;
      }
      offset += Character.charCount(codePoint);
    }

    return sb.toString();
  }
}
