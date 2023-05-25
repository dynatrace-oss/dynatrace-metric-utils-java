// TODO: continue with the absolute unit serialization
// TODO: Fix the bugs around using separate backing fields instead of a list for description and unit

package com.dynatrace.metric.util;

import com.dynatrace.metric.util.validation.CodePoints;
import com.dynatrace.metric.util.validation.DimensionValueValidator;
import com.dynatrace.metric.util.validation.UnitValidator;
import org.apache.commons.lang3.StringUtils;
import sun.jvm.hotspot.oops.Metadata;

import java.util.ArrayList;

import static com.dynatrace.metric.util.MetadataConstants.Limits.MAX_DESCRIPTION_LENGTH;

public class MetadataLineBuilderImpl
  implements MetadataLineBuilder.MetricKeyStep,
  MetadataLineBuilder.TypeStep,
  MetadataLineBuilder.DescriptionStep,
  MetadataLineBuilder.UnitStep,
  MetadataLineBuilder.BuildStep {

  private String description;
  private String unit;

  private String metricKey;
  private String payloadType;
  private int dimensionsLength = 0;

  // An internal state used to short-circuit calls to the builder methods
  // when an error has been previously detected.
  private boolean hasError = false;

  private MetadataLineBuilderImpl() {
  }

  /**
   * Creates a {@link MetadataLineBuilderImpl} that uses Noop implementations
   *
   * @return The {@link MetadataLineBuilder.MetricKeyStep} that is used to set the metric key.
   */
  public static MetadataLineBuilder.MetricKeyStep newBuilder() {
    return new MetadataLineBuilderImpl();
  }


  public MetadataLineBuilder.TypeStep metricKey(String key) {
    metricKey = normalizeMetricKey(key);
    return this;
  }

  @Override
  public MetadataLineBuilder.DescriptionStep gauge() {
    if (this.hasError) {
      return this;
    }

    this.payloadType = MetadataConstants.Payload.TYPE_GAUGE;
    return this;
  }

  @Override
  public MetadataLineBuilder.DescriptionStep counter() {
    if (this.hasError) {
      return this;
    }

    this.payloadType = MetadataConstants.Payload.TYPE_COUNT;
    return this;
  }

  @Override
  public MetadataLineBuilder.UnitStep description(String description) {
    if (this.hasError || StringUtils.isBlank(description) || DimensionValueValidator.isEmptyQuoted(description)) {
      return this;
    }
    // TODO Constants for MAX_DIMENSION_VALUE
    description = LineNormalizer.normalizeDimensionValue(description, MAX_DESCRIPTION_LENGTH);

    // TODO Maybe Log here when description was normalized?

    // +1 refers to the = char
    this.dimensionsLength += MetadataConstants.Dimensions.DESCRIPTION_KEY.length() + 1 + description.length();
    return this;
  }

  @Override
  public MetadataLineBuilder.UnitStep noDescription() {
    return this;
  }

  @Override
  public MetadataLineBuilder.UnitStep unit(String unit) {
    if (this.hasError || StringUtils.isBlank(unit) || DimensionValueValidator.isEmptyQuoted(unit)) {
      return this;
    }

    // For now, we don't support the '1' default dimensionless unit value from OTel.
    // Initially, we thought of always mapping this to 'ratio' in Dynatrace but the OTel spec is
    // somewhat vague, and it's not entirely clear if this mapping is correct in all cases.
    // Once the spec is clearer, we can revisit this.
    // TODO: check if this is the case for micrometer too
    if (unit.equals(MetadataConstants.Units.OTEL_UTILIZATION_DEFAULT_UNIT)) {
      return this;
    }

    if (!UnitValidator.isValidUnit(unit)) {

      return this;
    }

    String dtUnit = unit;
    if (unit.equals(MetadataConstants.Units.OTEL_PERCENT_UNIT_SYMBOL)) {
      // Replaces the '%' unit from OTel with Percent in Dynatrace
      dtUnit = MetadataConstants.Units.DT_PERCENT;
    } else if (unit.codePointAt(0) == CodePoints.CURLY_BRACKET_OPEN) {
      // no need to check for `}`, this would be caught be the validator above
      // Remove { } from units - {packets} => packets
      dtUnit = unit.substring(1, unit.length() - 1);
    }

    String dimension = MetadataConstants.Dimensions.UNIT_KEY + (char) CodePoints.EQUALS + dtUnit;
    this.dimensions.add(dimension);
    this.dimensionsLength += dimension.length();

    return this;
  }

  @Override
  public MetadataLineBuilder.UnitStep noUnit() {
    return this;
  }

  @Override
  public String build() {
    if (this.hasError || this.dimensions.isEmpty()) {
      // at least one dimension is required (unit/description)
      return null;
    }

    // The count of chars needed to compose the line
    // EBNF: '#'identifier type dimension (',' dimension )*
    int separationCharLength = Character.charCount(CodePoints.NUMBER_SIGN) +
      Character.charCount(CodePoints.BLANK) * 2 +
      this.dimensions.size() - 1; // a comma is required only if the number of dims is > 1

    StringBuilder lineBuilder = new StringBuilder(
      this.metricKey.length() + this.payloadType.length() + separationCharLength + this.dimensionsLength);

    lineBuilder
      .appendCodePoint(CodePoints.NUMBER_SIGN)
      .append(this.metricKey)
      .appendCodePoint(CodePoints.BLANK)
      .append(this.payloadType)
      .appendCodePoint(CodePoints.BLANK);

    // add the dimension(s) to the line
    for (int i = 0; i < this.dimensions.size(); i++) {
      if (i > 0) {
        lineBuilder.appendCodePoint(CodePoints.COMMA);
      }
      lineBuilder.append(this.dimensions.get(i));
    }

    return lineBuilder.toString();
  }

  private String normalizeMetricKey(String key) {
    // TODO: normalize key
    return key;
  }
}
