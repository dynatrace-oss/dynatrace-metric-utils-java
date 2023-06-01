package com.dynatrace.metric.util.validation;

import com.dynatrace.metric.util.MetadataConstants;

public final class UnitValidator {

  private UnitValidator() {}

  /**
   * Checks if the provided unit is valid according to the spec.
   *
   * @param unit The unit.
   * @return True if the unit is valid, false otherwise.
   */
  public static boolean isValidUnit(String unit) {
    if (unit == null || unit.isEmpty()) {
      return false;
    }

    final int length = unit.length();

    if (length > MetadataConstants.Limits.MAX_UNIT_LENGTH) {
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

  private static boolean isAllowedChar(int codePoint) {
    // Uppercase and lowercase letters, numbers and characters '%','[',']','/' and '_'
    return isLetter(codePoint)
        || isNumber(codePoint)
        || codePoint == CodePoints.PERCENT_SIGN
        || codePoint == CodePoints.OPEN_SQUARE_BRACKET
        || codePoint == CodePoints.CLOSE_SQUARE_BRACKET
        || codePoint == CodePoints.FORWARDSLASH
        || codePoint == CodePoints.UNDERSCORE;
  }

  private static boolean isLetter(int codePoint) {
    return (codePoint >= CodePoints.A_LOWERCASE && codePoint <= CodePoints.Z_LOWERCASE)
        || (codePoint >= CodePoints.A_UPPERCASE && codePoint <= CodePoints.Z_UPPERCASE);
  }

  private static boolean isNumber(int codePoint) {
    return codePoint >= CodePoints.ZERO && codePoint <= CodePoints.NINE;
  }
}
