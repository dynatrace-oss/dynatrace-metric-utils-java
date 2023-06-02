package com.dynatrace.metric.util;

import static com.dynatrace.metric.util.MetadataConstants.Limits.MAX_DESCRIPTION_LENGTH;

class MetadataLineFactory {

  private MetadataLineFactory() { }

  static String createMetadataLine(
      String metricKey, String description, String unit, String payloadType) {
    String normalizedDescription = null;

    int builderLength = 0;
    // if description is not empty or null, try normalize
    if (description != null && !description.isEmpty()) {
      normalizedDescription =
          StringValueNormalizer.normalizeStringValue(description, MAX_DESCRIPTION_LENGTH);
      builderLength += normalizedDescription.length();
    }

    if (UnitValidator.isValidUnit(unit)) {
      builderLength += unit.length();
    }

    // neither desc nor unit are set
    if (builderLength == 0) {
      return null;
    }

    builderLength +=
        Character.charCount(CodePoints.NUMBER_SIGN)
            + Character.charCount(CodePoints.BLANK) * 2
            // a comma may be required if description and unit are set
            + Character.charCount(CodePoints.COMMA);

    StringBuilder lineBuilder = new StringBuilder(builderLength);

    lineBuilder
        .appendCodePoint(CodePoints.NUMBER_SIGN)
        .append(metricKey)
        .appendCodePoint(CodePoints.BLANK)
        .append(payloadType)
        .appendCodePoint(CodePoints.BLANK);

    if (normalizedDescription != null) {
      lineBuilder
          .append(MetadataConstants.Dimensions.DESCRIPTION_KEY)
          .appendCodePoint(CodePoints.EQUALS)
          .append(normalizedDescription);
      if (unit != null) {
        lineBuilder.appendCodePoint(CodePoints.COMMA);
      }
    }

    if (unit != null) {
      lineBuilder
          .append(MetadataConstants.Dimensions.UNIT_KEY)
          .appendCodePoint(CodePoints.EQUALS)
          .append(unit);
    }

    return lineBuilder.toString();
  }
}
