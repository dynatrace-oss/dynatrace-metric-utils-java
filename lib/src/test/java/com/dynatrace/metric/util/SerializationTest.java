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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationTest {
  private static final String METRIC_NAME = "my.metric";
  private static MetricLineBuilder.TypeStep metricLineBuilder;

  @BeforeEach
  public void setUp() throws MetricException {
    metricLineBuilder = MetricLineBuilder.create().metricKey(METRIC_NAME);
  }

  @Test
  public void testFormatDouble() {
    assertEquals("0", Normalizer.doubleToString(0));
    assertEquals("0", Normalizer.doubleToString(-0));
    assertEquals("0", Normalizer.doubleToString(.000000000000000));
    assertEquals("1", Normalizer.doubleToString(1));
    assertEquals("1", Normalizer.doubleToString(1.00000000000000));
    assertEquals("1.23456789", Normalizer.doubleToString(1.23456789000));
    assertEquals("1.1234567890123457", Normalizer.doubleToString(1.1234567890123456789));
    assertEquals("-1.23456789", Normalizer.doubleToString(-1.23456789000));
    assertEquals("-1.1234567890123457", Normalizer.doubleToString(-1.1234567890123456789));
    assertEquals("200", Normalizer.doubleToString(200));
    assertEquals("200", Normalizer.doubleToString(200.00000000));
    assertEquals("10000000000", Normalizer.doubleToString(1e10));
    assertEquals("1000000000000", Normalizer.doubleToString(1_000_000_000_000d));
    assertEquals("1234567000000", Normalizer.doubleToString(1_234_567_000_000d));
    assertEquals("1.234567000000123E12", Normalizer.doubleToString(1_234_567_000_000.123));
    assertEquals("1.7976931348623157E308", Normalizer.doubleToString(Double.MAX_VALUE));
    assertEquals("4.9E-324", Normalizer.doubleToString(Double.MIN_VALUE));
    // these should never happen in the execution of the program, but these tests ensure that
    // nothing is thrown from the format method.
    assertEquals("NaN", Normalizer.doubleToString(Double.NaN));
    assertEquals("Infinity", Normalizer.doubleToString(Double.POSITIVE_INFINITY));
    assertEquals("-Infinity", Normalizer.doubleToString(Double.NEGATIVE_INFINITY));
  }

  @Test
  public void testSerializedDoubleCounterValue() throws MetricException {
    assertTrue(metricLineBuilder.count().delta(0.000).build().contains("count,delta=0"));
    assertTrue(metricLineBuilder.count().delta(100.123).build().contains("count,delta=100.123"));
    assertTrue(
        metricLineBuilder
            .count()
            .delta(100.123456789)
            .build()
            .contains("count,delta=100.123456789"));
    assertTrue(metricLineBuilder.count().delta(-10.123).build().contains("count,delta=-10.123"));
  }

  @Test
  public void testSerializedDoubleSummaryValue() throws MetricException {
    assertTrue(
        metricLineBuilder
            .gauge()
            .summary(0.123, 10.321, 20, 10)
            .build()
            .contains("gauge,min=0.123,max=10.321,sum=20,count=10"));
    assertTrue(
        metricLineBuilder
            .gauge()
            .summary(0.000, 0.000, 0.000, 0)
            .build()
            .contains("gauge,min=0,max=0,sum=0,count=0"));
  }

  @Test
  public void testSerializedDoubleGaugeValue() throws MetricException {
    assertTrue(metricLineBuilder.gauge().value(0.000).build().contains("gauge,0"));
    assertTrue(metricLineBuilder.gauge().value(123.456).build().contains("gauge,123.456"));
    assertTrue(metricLineBuilder.gauge().value(-123.456).build().contains("gauge,-123.456"));
  }

  @Test
  void testSerializeValidDimensions() throws MetricException {
    Map<String, String> dimensions = new HashMap<>();
    dimensions.put("valid1", "value1");
    dimensions.put("valid2", "value2");

    String actual =
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .dimensions(dimensions)
            .count()
            .delta(1)
            .build();

    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));
    assertUnorderedEquals(Arrays.asList("valid1=value1", "valid2=value2"), actualDims);
  }

  @Test
  void testSerializeWithNormalizedDimensions() throws MetricException {
    Map<String, String> dimensions = new HashMap<>();
    dimensions.put("~!@LEADING", "value1");
    dimensions.put("TRAILING!@#", "value2");

    String actual =
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .dimensions(dimensions)
            .count()
            .delta(1)
            .build();

    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));
    assertUnorderedEquals(Arrays.asList("_leading=value1", "trailing_=value2"), actualDims);
  }

  @Test
  void testSerializeWithNormalizationRemoveInvalidDimensionKey() throws MetricException {
    Map<String, String> dimensions = new HashMap<>();
    dimensions.put("valid", "value");
    dimensions.put("!@@$", "underscore");
    dimensions.put("another_valid", "value");

    String actual =
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .dimensions(dimensions)
            .count()
            .delta(1)
            .build();

    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));
    assertUnorderedEquals(
        Arrays.asList("valid=value", "_=underscore", "another_valid=value"),
        actualDims); // could only be two
  }

  @Test
  void testSerializeMergeMultipleDimensionListsNoCollision() throws MetricException {
    Map<String, String> dimensions1 = Collections.singletonMap("default", "dimension");
    Map<String, String> dimensions2 = Collections.singletonMap("label", "dimension");

    String actual =
        MetricLineBuilder.create(
                MetricLinePreConfiguration.builder().defaultDimensions(dimensions1).build())
            .metricKey(METRIC_NAME)
            .dimensions(dimensions2)
            .count()
            .delta(1)
            .build();

    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));
    assertUnorderedEquals(Arrays.asList("default=dimension", "label=dimension"), actualDims);
  }

  @Test
  void testSerializeMergeMultipleDimensionListsOverwriting() throws MetricException {
    Map<String, String> dimensions1 = new HashMap<>();
    dimensions1.put("dimension1", "default1");
    dimensions1.put("dimension2", "default2");
    Map<String, String> dimensions2 = Collections.singletonMap("dimension1", "overwritten");

    String actual =
        MetricLineBuilder.create(
                MetricLinePreConfiguration.builder().defaultDimensions(dimensions1).build())
            .metricKey(METRIC_NAME)
            .dimensions(dimensions2)
            .count()
            .delta(1)
            .build();

    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));
    assertUnorderedEquals(
        Arrays.asList("dimension1=overwritten", "dimension2=default2"), actualDims);
  }

  @Test
  void testSerializeEmptyDimensions() throws MetricException {
    Map<String, String> dimensions = new HashMap<>();

    assertEquals(
        "my.metric count,delta=1",
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .dimensions(dimensions)
            .count()
            .delta(1)
            .build());
  }

  @Test
  void testSerializeOneDimension() throws MetricException {
    String actual =
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .dimensions(Collections.singletonMap("key", "value"))
            .count()
            .delta(1)
            .build();

    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));
    assertUnorderedEquals(Collections.singletonList("key=value"), actualDims);
  }

  private static void assertUnorderedEquals(
      Collection<String> expected, Collection<String> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
    assertTrue(actual.containsAll(expected));
    assertNotSame(expected, actual);
  }
}
