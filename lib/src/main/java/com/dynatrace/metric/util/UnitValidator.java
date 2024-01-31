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

/** Offers validation methods for unit values */
final class UnitValidator {

  private UnitValidator() {}

  /**
   * Checks if the provided unit is valid.
   *
   * @param unit The unit.
   * @return True if the unit is valid, false otherwise.
   */
  static boolean isValidUnit(String unit) {
    if (unit == null || unit.isEmpty()) {
      return false;
    }

    final int length = unit.length();
    if (length > MetadataLineConstants.Limits.MAX_UNIT_LENGTH) {
      return false;
    }

    for (int offset = 0; offset < length; ) {
      final int codePoint = unit.codePointAt(offset);
      if (!isAllowedChar(codePoint)) {
        return false;
      }
      offset += Character.charCount(codePoint);
    }
    return true;
  }

  /**
   * Checks if a codepoint is an allowed char.
   *
   * @param codePoint The codepoint.
   * @return True if the codepoint is an allowed char, otherwise false.
   */
  private static boolean isAllowedChar(int codePoint) {
    // Uppercase and lowercase letters, numbers and these special characters (not the comma): %, [,
    // ], {, }, /, _
    return isLetter(codePoint)
        || isNumber(codePoint)
        || codePoint == CodePoints.PERCENT_SIGN
        || codePoint == CodePoints.OPEN_SQUARE_BRACKET
        || codePoint == CodePoints.CLOSED_SQUARE_BRACKET
        || codePoint == CodePoints.OPEN_CURLY_BRACKET
        || codePoint == CodePoints.CLOSED_CURLY_BRACKET
        || codePoint == CodePoints.FORWARD_SLASH
        || codePoint == CodePoints.UNDERSCORE;
  }

  /**
   * Checks if the codepoint is either a lowercase or uppercase letter.
   *
   * @param codePoint The codepoint.
   * @return True if the codepoint is a letter, otherwise false.
   */
  private static boolean isLetter(int codePoint) {
    return (codePoint >= CodePoints.A_LOWERCASE && codePoint <= CodePoints.Z_LOWERCASE)
        || (codePoint >= CodePoints.A_UPPERCASE && codePoint <= CodePoints.Z_UPPERCASE);
  }

  /**
   * Checks if the codepoint is a number.
   *
   * @param codePoint The codepoint.
   * @return True if the codepoint is a number, otherwise false.
   */
  private static boolean isNumber(int codePoint) {
    return codePoint >= CodePoints.ZERO && codePoint <= CodePoints.NINE;
  }
}
