package com.dynatrace.metric.util;

/**
 * Utility class containing methods normalize strings according to the Dynatrace specification
 */
class StringValueNormalizer {

  private static final String EMPTY_QUOTED_STRING = "\"\"";

  private StringValueNormalizer() {}

  /**
   * Applies normalization to the provided String.
   *
   * @param value The string to normalize.
   * @param maxStringValueLength The maximum length of the string.
   * @return The {@link String result}, containing the potentially normalized value along with any
   *     warning encountered.
   */
  public static String normalizeStringValue(String value, int maxStringValueLength) {
    if (value == null || value.isEmpty()) {
      return null;
    }

    boolean isQuoted =
        value.startsWith(CodePoints.QUOTATION_MARK) && value.endsWith(CodePoints.QUOTATION_MARK);
    if (isQuoted) {
      return normalizeQuotedStringValue(value, maxStringValueLength);
    }
    return normalizeUnquotedStringValue(value, maxStringValueLength);
  }

  /**
   * Applies normalization to the string.
   *
   * @param value The string.
   * @param maxStringValueLength The maximum value of the string.
   * @return The {@link String result}, containing the potentially normalized value along with any
   *     warning encountered.
   */
  private static String normalizeUnquotedStringValue(String value, int maxStringValueLength) {
    if (!StringValueValidator.normalizationRequiredStringValue(value, maxStringValueLength)) {
      return value;
    }

    StringBuilder sb = new StringBuilder();
    final int length = value.length();
    boolean wasNormalizedBefore = false;

    for (int offset = 0; offset < length; ) {
      final int codePoint = value.codePointAt(offset);

      if (StringValueValidator.isInvalidCodePoint(codePoint)) {
        if (wasNormalizedBefore) {
          offset += Character.charCount(codePoint);
          continue;
        }
        if (!StringValueValidator.canAppendToValue(
            sb.length(), CodePoints.UNDERSCORE, false, false, maxStringValueLength)) {
          break;
        }
        sb.appendCodePoint(CodePoints.UNDERSCORE);
        wasNormalizedBefore = true;
      } else {
        boolean shouldEscape = StringValueValidator.shouldEscapeString(codePoint);
        boolean canAppend =
            StringValueValidator.canAppendToValue(
                sb.length(), codePoint, false, shouldEscape, maxStringValueLength);

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
   * Applies normalization to the quoted string.
   *
   * @param value The quoted.
   * @param maxStringValueLength The maximum value to compare too. E.g. The maximum value for
   *     dimensions.
   * @return The {@link String result}, containing the potentially normalized value along with any
   *     warning encountered.
   */
  private static String normalizeQuotedStringValue(String value, int maxStringValueLength) {
    if (value.equals(EMPTY_QUOTED_STRING)){
      return null;
    }

    if (!StringValueValidator.normalizationRequiredQuotedStringValue(
        value, maxStringValueLength)) {
      return value;
    }

    StringBuilder sb = new StringBuilder();
    final int length = value.length();
    boolean wasNormalizedBefore = false;
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
            if (wasNormalizedBefore) {
              break;
            }
            if (!StringValueValidator.canAppendToValue(
                sb.length(), CodePoints.UNDERSCORE, true, false, maxStringValueLength)) {
              wasTruncated = true;
              break;
            }
            sb.appendCodePoint(CodePoints.UNDERSCORE);
            wasNormalizedBefore = true;
            break;
          }
          boolean shouldEscape = StringValueValidator.shouldEscapeQuotedString(codePoint);
          boolean canAppend =
              StringValueValidator.canAppendToValue(
                  sb.length(), codePoint, true, shouldEscape, maxStringValueLength);

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
