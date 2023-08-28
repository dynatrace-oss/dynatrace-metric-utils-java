/**
 * Copyright 2021 Dynatrace LLC
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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class MetricBuilderTest {

  @Test
  void testSetDoubleGauge() throws MetricException {
    String expected = "name gauge,1.23";
    String actual = MetricLineBuilder.create().metricKey("name").gauge().value(1.23).build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDoubleCounterValueDelta() throws MetricException {
    String expected = "name count,delta=1.23";
    String actual = MetricLineBuilder.create().metricKey("name").count().delta(1.23).build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDoubleSummaryValue() throws MetricException {
    String expected = "name gauge,min=1.23,max=10.34,sum=20.45,count=7";
    String actual =
        MetricLineBuilder.create().metricKey("name").gauge().summary(1.23, 10.34, 20.45, 7).build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetInvalidSummaryValue() {
    Double validValue = 1.23d;
    List<Double> values =
        Arrays.asList(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, validValue);
    for (Double minVal : values) {
      for (Double maxVal : values) {
        for (Double sumVal : values) {
          if (minVal.equals(validValue) && maxVal.equals(validValue) && sumVal.equals(validValue)) {
            // skip the case where min, max, and count are the valid number. All other combinations
            // where at least one of the values is invalid should throw.
            continue;
          }

          assertThrows(
              MetricException.class,
              () ->
                  MetricLineBuilder.create()
                      .metricKey("my.metric.Key")
                      .gauge()
                      .summary(minVal, maxVal, sumVal, 1));
        }
      }
    }
  }

  @Test
  void testSetInvalidCounterValueDelta() {
    List<Double> values =
        Arrays.asList(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    for (Double val : values) {
      assertThrows(
          MetricException.class,
          () -> MetricLineBuilder.create().metricKey("my.metric.Key").count().delta(val));
    }
  }

  @Test
  void testSetInvalidGaugeValue() {
    List<Double> values =
        Arrays.asList(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    for (Double val : values) {
      assertThrows(
          MetricException.class,
          () -> MetricLineBuilder.create().metricKey("my.metric.Key").gauge().value(val));
    }
  }

  @Test
  void testNullOrEmptyMetricKey() {
    assertThrows(MetricException.class, () -> MetricLineBuilder.create().metricKey(""));
    assertThrows(MetricException.class, () -> MetricLineBuilder.create().metricKey(null));
  }

  @Test
  void testInvalidMetricKey() {
    assertThrows(MetricException.class, () -> MetricLineBuilder.create().metricKey("."));
  }

  @Test
  void testNameContainsInvalidChars() throws MetricException {
    String expected = "_ count,delta=1";
    String actual = MetricLineBuilder.create().metricKey("~@#$").count().delta(1).build();
    assertEquals(expected, actual);
  }

  @Test
  void testPrefix() throws MetricException {
    String expected = "prefix.name count,delta=1";

    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetPrefixTwice() {
    MetricLinePreConfiguration.MetricLinePreConfigurationBuilder preConfigBuilder =
        assertDoesNotThrow(() -> MetricLinePreConfiguration.builder().prefix("prefix"));
    assertDoesNotThrow(() -> preConfigBuilder.prefix("prefix2"));

    MetricLinePreConfiguration config = assertDoesNotThrow(() -> preConfigBuilder.build());
    assertEquals(config.getPrefix(), "prefix2");
  }

  @Test
  void testPrefixWithTrailingDot() throws MetricException {
    String expected = "prefix.name count,delta=1";
    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix.").build())
            .metricKey("name")
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testPrefixContainsInvalidChars() throws MetricException {
    String expected = "_.name count,delta=1";
    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("~@#$").build())
            .metricKey("name")
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testPrefixAndNameContainInvalidChars() throws MetricException {
    String expected = "_._ count,delta=1";

    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("!@#").build())
            .metricKey("~@#$")
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetTimestamp() throws MetricException {
    String expected = "prefix.name count,delta=1 1616580000123";
    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .count()
            .delta(1)
            .timestamp(Instant.ofEpochMilli(1616580000123L))
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetNullTimestamp() throws MetricException {
    String expected = "prefix.name count,delta=1";
    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .count()
            .delta(1)
            .timestamp(null)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetInvalidTimestampsSeconds() throws MetricException {
    // timestamp specified in seconds
    String expected = "prefix.name count,delta=1";
    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .count()
            .delta(1)
            .timestamp(Instant.ofEpochMilli(1616580000L))
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetInvalidTimestampsNanoseconds() throws MetricException {
    // timestamp specified in nanoseconds
    String expected = "prefix.name count,delta=1";
    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .count()
            .delta(1)
            .timestamp(Instant.ofEpochMilli(1616580000000000L))
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDimensions() throws MetricException {
    String expected = "prefix.name,dim1=val1 count,delta=1";

    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .dimensions(Collections.singletonMap("dim1", "val1"))
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDimensionsTwice() throws MetricException {
    String expectedBase = "prefix.name count,delta=1";
    List<String> expectedDims = Arrays.asList("dim1=val1", "dim2=val2");

    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .dimensions(Collections.singletonMap("dim1", "val1"))
            .dimensions(Collections.singletonMap("dim2", "val2"))
            .count()
            .delta(1)
            .build();

    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @ParameterizedTest(name = "{index}: {0}, key: {1}, value: {2}")
  @MethodSource("provideNullOrEmptyDimensions")
  void testSetNullOrEmptyDimensions(String name, String key, String value) throws MetricException {
    String expected = "prefix.name count,delta=1";

    assertEquals(
        expected,
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .dimensions(Collections.singletonMap(key, value))
            .count()
            .delta(1)
            .build());
  }

  @Test
  void testInvalidDimensionNotAdded() throws MetricException {
    String expected = "prefix.name count,delta=1";

    Map<String, String> dimensions = Collections.singletonMap(".", "value");

    String actual =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .dimensions(dimensions)
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testJustDefaultDimensions() throws MetricException {
    String expected = "prefix.name,defdim1=val1 count,delta=1";
    String actual =
        MetricLineBuilder.create(
                MetricLinePreConfiguration.builder()
                    .prefix("prefix")
                    .defaultDimensions(Collections.singletonMap("defdim1", "val1"))
                    .build())
            .metricKey("name")
            .count()
            .delta(1)
            .build();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDefaultDimensionsTwice() {
    MetricLinePreConfiguration.MetricLinePreConfigurationBuilder preConfigurationBuilder =
        assertDoesNotThrow(
            () ->
                MetricLinePreConfiguration.builder()
                    .defaultDimensions(Collections.singletonMap("defdim1", "val1")));

    assertDoesNotThrow(
        () ->
            preConfigurationBuilder.defaultDimensions(
                Collections.singletonMap("newdefdim1", "newVal1")));

    MetricLinePreConfiguration preConfig =
        assertDoesNotThrow(() -> preConfigurationBuilder.build());

    assertEquals(preConfig.getDefaultDimensions().get("newdefdim1"), "newVal1");
    assertEquals(preConfig.getDefaultDimensions().size(), 1);
  }

  @Test
  void testSetDefaultDimensionsWithMoreThan50Entries() {
    Map<String, String> dimensions = new HashMap<>();
    for (int i = 0; i < 50; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.put(key, val);
    }

    assertDoesNotThrow(() -> MetricLinePreConfiguration.builder().defaultDimensions(dimensions));

    dimensions.put("dim51", "val51");
    assertThrows(
        MetricException.class,
        () -> MetricLinePreConfiguration.builder().defaultDimensions(dimensions));
  }

  @Test
  void testSetDimensionsWithMoreThan50Entries() {
    Map<String, String> dimensions = new HashMap<>();
    for (int i = 0; i < 50; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.put(key, val);
    }

    assertDoesNotThrow(
        () -> MetricLineBuilder.create().metricKey("my.metric").dimensions(dimensions));

    dimensions.put("dim51", "val51");
    assertThrows(
        MetricException.class,
        () -> MetricLineBuilder.create().metricKey("my.metric").dimensions(dimensions));
  }

  @Test
  void testSetDimensionMoreThan50Times() throws MetricException {
    MetricLineBuilder.TypeStep builder = MetricLineBuilder.create().metricKey("my.metric");

    for (int i = 0; i < 50; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      assertDoesNotThrow(() -> builder.dimension(key, val));
    }

    assertThrows(MetricException.class, () -> builder.dimension("dim.to.fail", "val"));
  }

  @Test
  void testSetDimensionLimitExceedWithValidPreConfiguration() {
    Map<String, String> dimensions = new HashMap<>();
    for (int i = 0; i < 50; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.put(key, val);
    }

    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(DynatraceMetadataEnricher::getDynatraceMetadata)
          .thenReturn(Collections.singletonMap("dim3", "metadataVal"));

      MetricLinePreConfiguration.MetricLinePreConfigurationBuilder preConfig =
          assertDoesNotThrow(
              () -> MetricLinePreConfiguration.builder().defaultDimensions(dimensions));

      // preConfig will still have exact 50 dimensions (dim3 will be overwritten)
      preConfig.dynatraceMetadataDimensions();

      MetricLinePreConfiguration config = assertDoesNotThrow(() -> preConfig.build());
      assertThrows(
          MetricException.class,
          () ->
              MetricLineBuilder.create(config)
                  .metricKey("my.metric")
                  .dimensions(Collections.singletonMap("key", "value")));
      assertThrows(
          MetricException.class,
          () -> MetricLineBuilder.create(config).metricKey("my.metric").dimension("key", "value"));
    }
  }

  @Test
  void testPreConfigurationExceedsDimensionLimitDuringBuild() {
    Map<String, String> dimensions = new HashMap<>();
    for (int i = 0; i < 50; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.put(key, val);
    }

    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(DynatraceMetadataEnricher::getDynatraceMetadata)
          .thenReturn(Collections.singletonMap("dim51", "metadataVal"));

      MetricLinePreConfiguration.MetricLinePreConfigurationBuilder preConfig =
          assertDoesNotThrow(
              () -> MetricLinePreConfiguration.builder().defaultDimensions(dimensions));

      // adding the preConfig should exceed the dimension limit
      preConfig.dynatraceMetadataDimensions();

      assertThrows(MetricException.class, () -> preConfig.build());
    }
  }

  @Test
  void testPreConfigurationContainsExactDimensionLimit() {
    Map<String, String> dimensions = new HashMap<>();
    for (int i = 0; i < 50; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.put(key, val);
    }

    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(DynatraceMetadataEnricher::getDynatraceMetadata)
          .thenReturn(Collections.singletonMap("dim3", "metadataVal"));

      MetricLinePreConfiguration.MetricLinePreConfigurationBuilder preConfig =
          assertDoesNotThrow(
              () -> MetricLinePreConfiguration.builder().defaultDimensions(dimensions));

      // preConfig will still have exact 50 dimensions (dim3 will be overwritten)
      preConfig.dynatraceMetadataDimensions();

      assertDoesNotThrow(() -> preConfig.build());
    }
  }

  @ParameterizedTest(name = "{index}: {0}, key: {1}, value: {2}")
  @MethodSource("provideNullOrEmptyDimensions")
  void testSetNullOrEmptyDefaultDimensions(String name, String key, String value)
      throws MetricException {
    String expected = "prefix.name count,delta=1";

    assertEquals(
        expected,
        MetricLineBuilder.create(
                MetricLinePreConfiguration.builder()
                    .prefix("prefix")
                    .defaultDimensions(Collections.singletonMap(key, value))
                    .build())
            .metricKey("name")
            .count()
            .delta(1)
            .build());
  }

  @Test
  void testSetDefaultAndDynamicDimensions() throws MetricException {
    String expectedBase = "prefix.name count,delta=1";
    List<String> expectedDims = Arrays.asList("defaultdim1=defaultVal1", "dim1=val1");

    String actual =
        MetricLineBuilder.create(
                MetricLinePreConfiguration.builder()
                    .prefix("prefix")
                    .defaultDimensions(Collections.singletonMap("defaultDim1", "defaultVal1"))
                    .build())
            .metricKey("name")
            .dimensions(Collections.singletonMap("dim1", "val1"))
            .count()
            .delta(1)
            .build();

    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @Test
  void testSetDefaultAndDynamicAndMetadataDimensions() throws MetricException {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(DynatraceMetadataEnricher::getDynatraceMetadata)
          .thenReturn(Collections.singletonMap("metadataDim1", "metadataVal1"));

      String expectedBase = "prefix.name count,delta=1";
      List<String> expectedDims =
          Arrays.asList("defaultdim1=defaultVal1", "dim1=val1", "metadatadim1=metadataVal1");

      String actual =
          MetricLineBuilder.create(
                  MetricLinePreConfiguration.builder()
                      .prefix("prefix")
                      .defaultDimensions(Collections.singletonMap("defaultDim1", "defaultVal1"))
                      .dynatraceMetadataDimensions()
                      .build())
              .metricKey("name")
              .dimensions(Collections.singletonMap("dim1", "val1"))
              .count()
              .delta(1)
              .build();

      String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
      List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

      assertEquals(expectedBase, actualBase);
      assertListsEqualIgnoreOrder(expectedDims, actualDims);
    }
  }

  @Test
  void testOverwriting() throws MetricException {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(DynatraceMetadataEnricher::getDynatraceMetadata)
          .thenReturn(Collections.singletonMap("dim3", "metadataVal3"));

      String expectedBase = "prefix.name count,delta=1";
      List<String> expectedDims =
          Arrays.asList("dim1=defaultVal1", "dim2=dynamicVal2", "dim3=metadataVal3");

      Map<String, String> defaultDimensions = new HashMap<>();
      defaultDimensions.put("dim1", "defaultVal1");
      defaultDimensions.put("dim2", "defaultVal2");
      defaultDimensions.put("dim3", "defaultVal3");

      Map<String, String> dimensions = new HashMap<>();
      dimensions.put("dim2", "dynamicVal2");
      dimensions.put("dim3", "dynamicVal3");

      String actual =
          MetricLineBuilder.create(
                  MetricLinePreConfiguration.builder()
                      .prefix("prefix")
                      .defaultDimensions(defaultDimensions)
                      .dynatraceMetadataDimensions()
                      .build())
              .metricKey("name")
              .dimensions(dimensions)
              .count()
              .delta(1)
              .build();

      String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
      List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

      assertEquals(expectedBase, actualBase);
      assertListsEqualIgnoreOrder(expectedDims, actualDims);
    }
  }

  @Test
  void testDontAddExistingMetadataInformation() throws MetricException {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(DynatraceMetadataEnricher::getDynatraceMetadata)
          .thenReturn(Collections.singletonMap("dim1", "metadataVal1"));

      String expectedLine = "prefix.name count,delta=1";

      String actual =
          MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
              .metricKey("name")
              .count()
              .delta(1)
              .build();

      assertEquals(expectedLine, actual);
    }
  }

  @Test
  void testCreateMetadataLineWithUnit() throws MetricException {
    MetricLineBuilder.GaugeStep builder =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .gauge();

    assertEquals("prefix.name gauge,3", builder.value(3.).build());
    assertEquals("#prefix.name gauge dt.meta.unit=unit", builder.metadata().unit("unit").build());
  }

  @Test
  void testCreateMetadataLineWithDescription() throws MetricException {
    MetricLineBuilder.GaugeStep builder =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .gauge();

    assertEquals("prefix.name gauge,3", builder.value(3.).build());
    assertEquals(
        "#prefix.name gauge dt.meta.description=my\\ description\\ goes\\ here",
        builder.metadata().description("my description goes here").build());
  }

  @Test
  void testCreateMetadataLineWithDisplayName() throws MetricException {
    MetricLineBuilder.GaugeStep builder =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .gauge();

    assertEquals("prefix.name gauge,3", builder.value(3.).build());
    assertEquals(
        "#prefix.name gauge dt.meta.displayName=my\\ displayName\\ goes\\ here",
        builder.metadata().displayName("my displayName goes here").build());
  }

  @Test
  void testCreateMetadataLineWithUnitAndDescriptionAndDisplayName() throws MetricException {
    MetricLineBuilder.GaugeStep builder =
        MetricLineBuilder.create(MetricLinePreConfiguration.builder().prefix("prefix").build())
            .metricKey("name")
            .gauge();

    assertEquals("prefix.name gauge,3", builder.value(3.).build());
    assertEquals(
        "#prefix.name gauge dt.meta.description=my\\ description\\ goes\\ here,dt.meta.unit=unit,dt.meta.displayName=displayName",
        builder
            .metadata()
            .description("my description goes here")
            .unit("unit")
            .displayName("displayName")
            .build());
  }

  private void assertListsEqualIgnoreOrder(List<String> expected, List<String> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
  }

  private static Stream<Arguments> provideNullOrEmptyDimensions() {
    return Stream.of(
        Arguments.of("empty key - empty value", "", ""),
        Arguments.of("valid key - null value", "dim", null),
        Arguments.of("valid key - empty value", "dim", ""),
        Arguments.of("null key - empty value", null, ""),
        Arguments.of("empty key - valid value", "", "val"),
        Arguments.of("null key - valid value", null, "val"),
        Arguments.of("empty key - null value", "", null),
        Arguments.of("null key - null value", null, null));
  }
}
