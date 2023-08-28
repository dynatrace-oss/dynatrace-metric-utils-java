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

/** Offers validation methods for Metric Key. */
final class MetricKeyValidator {

  private MetricKeyValidator() {}

  /**
   * Checks the codepoint against the allowed chars for the start of the metric key.
   *
   * @param codePoint The codepoint.
   * @return true if it's a valid character for the start of the metric key, false otherwise.
   */
  static boolean isValidFirstSectionStartCharacter(int codePoint) {
    return codePoint >= CodePoints.A_LOWERCASE && codePoint <= CodePoints.Z_LOWERCASE
        || codePoint >= CodePoints.A_UPPERCASE && codePoint <= CodePoints.Z_UPPERCASE
        || codePoint == CodePoints.UNDERSCORE;
  }

  /**
   * Checks the codepoint against the allowed chars for the start of a consecutive section.
   *
   * @param codePoint The codepoint.
   * @return true if it's a valid character for the start of a consecutive section, false otherwise.
   */
  static boolean isValidConsecutiveSectionStartCharacter(int codePoint) {
    return isValidFirstSectionStartCharacter(codePoint) || isNumber(codePoint);
  }

  /**
   * Checks the codepoint against the allowed chars for the metric key section.
   *
   * @param codePoint The codepoint.
   * @return true if it's a valid character for the metric key section, false otherwise.
   */
  static boolean isValidSection(int codePoint) {
    return isValidConsecutiveSectionStartCharacter(codePoint) || isSpecialCharacter(codePoint);
  }

  /**
   * Checks if the codepoint is a metric key section separation char.
   *
   * @param codePoint The codepoint.
   * @return true if it's a metric key section separation char, false otherwise.
   */
  static boolean isSectionSeparator(int codePoint) {
    return codePoint == CodePoints.DOT;
  }

  /**
   * Iterates through the key once to find out if it needs to be normalized.
   *
   * @param key The metric key.
   * @return true if it needs to be normalized (truncated or replaced), false otherwise.
   */
  static boolean normalizationRequired(String key) {
    if (key == null || key.isEmpty()) {
      return true;
    }

    final int length = key.length();
    if (length > MetricLineConstants.Limits.MAX_METRIC_KEY_LENGTH) {
      return true;
    }

    State state = State.START;

    for (int offset = 0; offset < length; ) {
      final int codePoint = key.codePointAt(offset);
      switch (state) {
        case START:
          if (!isValidFirstSectionStartCharacter(codePoint)) {
            return true;
          }
          state = State.SECTION;
          break;
        case CONSECUTIVE_SECTION_START:
          if (!isValidConsecutiveSectionStartCharacter(codePoint)) {
            return true;
          }
          state = State.SECTION;
          break;
        case SECTION:
          if (isSectionSeparator(codePoint)) {
            // if the separator char is the last char, we need to normalize it
            if (offset + Character.charCount(codePoint) == length) {
              return true;
            }
            state = State.CONSECUTIVE_SECTION_START;
            break;
          }
          if (!isValidSection(codePoint)) {
            return true;
          }
          break;
      }
      offset += Character.charCount(codePoint);
    }
    return false;
  }

  /**
   * Checks if the codepoint is a number.
   *
   * @param codePoint The codepoint.
   * @return true if it's a number, false otherwise.
   */
  private static boolean isNumber(int codePoint) {
    return codePoint >= CodePoints.ZERO && codePoint <= CodePoints.NINE;
  }

  /**
   * Checks if the codepoint is a special character (e.g.: ",:)
   *
   * @param codePoint The codepoint.
   * @return true if it's a special character, false otherwise.
   */
  private static boolean isSpecialCharacter(int codePoint) {
    return codePoint == CodePoints.HYPHEN || codePoint == CodePoints.UNDERSCORE;
  }

  enum State {
    START,
    INVALID,
    CONSECUTIVE_SECTION_START,
    SECTION,
  }
}
