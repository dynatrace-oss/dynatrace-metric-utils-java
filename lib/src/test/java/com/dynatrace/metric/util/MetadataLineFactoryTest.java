package com.dynatrace.metric.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MetadataLineFactoryTest {
  private static final String METRIC_NAME = "my.metric";
  private static final String COUNT_TYPE = MetricType.COUNTER.toString();
  private static final String GAUGE_TYPE = MetricType.GAUGE.toString();

  private static String createExpectedLine(String description, String unit) {
    if (description != null && unit != null) {
      return String.format(
          "#%s " + // Metric name
              "%s " + // Metric type
              "%s=%s," + // description key = description value
              "%s=%s", // unit key = unit value
          METRIC_NAME,
          GAUGE_TYPE,
          MetadataConstants.Dimensions.DESCRIPTION_KEY,
          description,
          MetadataConstants.Dimensions.UNIT_KEY,
          unit);
    } else {
      if (description != null) {
        return String.format(
            "#%s " + // Metric name
                "%s " + // Metric type
                "%s=%s", // description key = description value
            METRIC_NAME, GAUGE_TYPE, MetadataConstants.Dimensions.DESCRIPTION_KEY, description);
      } else {
        return String.format(
            "#%s " + // Metric name
                "%s " + // Metric type
                "%s=%s", // unit key = unit value
            METRIC_NAME, GAUGE_TYPE, MetadataConstants.Dimensions.UNIT_KEY, unit);
      }
    }
  }

  @Test
  void whenUnitAndDescriptionAreEmpty_shouldReturnNull() {
    assertNull(MetadataLineFactory.createMetadataLine(METRIC_NAME, null, null, COUNT_TYPE));
    assertNull(MetadataLineFactory.createMetadataLine(METRIC_NAME, null, null, GAUGE_TYPE));
  }

  @Test
  void whenUnitIsNull_shouldCreateMetadataLineWithJustDescription() {
    assertEquals(
        createExpectedLine("my\\ description", null),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "my description", null, GAUGE_TYPE));
  }

  @Test
  void whenUnitIsEmpty_shouldCreateMetadataLineWithJustDescription() {
    assertEquals(
        createExpectedLine("my\\ description", null),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "my description", "", GAUGE_TYPE));
  }

  @Test
  void whenDescriptionIsNull_shouldCreateMetadataLineWithJustUnit() {
    assertEquals(
        createExpectedLine(null, "unit"),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, null, "unit", GAUGE_TYPE));
  }

  @Test
  void whenDescriptionIsEmpty_shouldCreateMetadataLineWithJustUnit() {
    assertEquals(
        createExpectedLine(null, "unit"),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "", "unit", GAUGE_TYPE));
  }

  @Test
  void whenDescriptionNeedsEscaping_shouldEscapeDescriptionValue() {
    assertEquals(
        createExpectedLine("my\\ description", null),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "my description", null, GAUGE_TYPE));
  }

  @Test
  void whenUnitAndDescriptionSet_shouldAddBoth() {
    assertEquals(
        createExpectedLine("description", "unit"),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "description", "unit", GAUGE_TYPE));
  }

  @Test
  void bothSet_invalidUnitIgnored() {
    assertEquals(
        createExpectedLine("description", null),
        MetadataLineFactory.createMetadataLine(
            METRIC_NAME, "description", "{invalid unit}", GAUGE_TYPE));
  }

  @Test
  void bothSet_invalidDescriptionIgnored() {
    assertEquals(
        createExpectedLine(null, "unit"),
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "", "unit", GAUGE_TYPE));
  }

  @Test
  void bothSet_bothInvalid_shouldReturnNull() {
    assertNull(
        MetadataLineFactory.createMetadataLine(METRIC_NAME, "", "{invalid unit}", GAUGE_TYPE));
  }

  @Test
  void onlyInvalidDescriptionSet_shouldReturnNull() {
    assertNull(MetadataLineFactory.createMetadataLine(METRIC_NAME, "", null, GAUGE_TYPE));
  }

  @Test
  void onlyInvalidUnitSet_shouldReturnNull() {
    assertNull(
        MetadataLineFactory.createMetadataLine(METRIC_NAME, null, "{invalid unit}", GAUGE_TYPE));
  }

  @Test
  void addOtherTypeToMetadataLineGeneration_lineIsCreatedWithOtherType() {
    String metricType = "othertype";
    String expected = String.format("#%s %s dt.meta.unit=unit", METRIC_NAME, metricType);
    assertEquals(
        expected, MetadataLineFactory.createMetadataLine(METRIC_NAME, null, "unit", metricType));
  }
}
