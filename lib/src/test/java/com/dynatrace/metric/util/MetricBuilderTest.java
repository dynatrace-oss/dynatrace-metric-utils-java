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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricBuilderTest {
  private void assertListsEqualIgnoreOrder(List<String> expected, List<String> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
  }

  @Test
  public void testSetLongCounterValueTotal() throws MetricException {
    String expected = "name count,1";
    String actual = Metric.builder("name").setLongCounterValueTotal(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetLongGaugeValue() throws MetricException {
    String expected = "name gauge,1";
    String actual = Metric.builder("name").setLongGaugeValue(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetLongCounterValueDelta() throws MetricException {
    String expected = "name count,delta=1";
    String actual = Metric.builder("name").setLongCounterValueDelta(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetLongSummaryValue() throws MetricException {
    String expected = "name gauge,min=1,max=10,sum=20,count=7";
    String actual = Metric.builder("name").setLongSummaryValue(1, 10, 20, 7).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetDoubleCounterValueTotal() throws MetricException {
    String expected = "name count,1.23";
    String actual = Metric.builder("name").setDoubleCounterValueTotal(1.23).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetDoubleGauge() throws MetricException {
    String expected = "name gauge,1.23";
    String actual = Metric.builder("name").setDoubleGaugeValue(1.23).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetDoubleCounterValueDelta() throws MetricException {
    String expected = "name count,delta=1.23";
    String actual = Metric.builder("name").setDoubleCounterValueDelta(1.23).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetDoubleSummaryValue() throws MetricException {
    String expected = "name gauge,min=1.23,max=10.34,sum=20.45,count=7";
    String actual = Metric.builder("name").setDoubleSummaryValue(1.23, 10.34, 20.45, 7).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testNoValueSet() {
    assertThrows(MetricException.class, () -> Metric.builder("name").serialize());
  }

  @Test
  public void testValueSetTwice() {
    assertThrows(
        MetricException.class,
        () ->
            Metric.builder("name")
                .setLongCounterValueTotal(1)
                .setDoubleCounterValueTotal(1.23)
                .serialize());
  }

  @Test
  public void testInvalidName() {
    assertThrows(
        MetricException.class,
        () -> Metric.builder("~@#$").setLongCounterValueTotal(1).serialize());
  }

  @Test
  public void testPrefix() throws MetricException {
    String expected = "prefix.name count,1";
    String actual =
        Metric.builder("name").setPrefix("prefix").setLongCounterValueTotal(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testInvalidPrefix() {
    assertThrows(
        MetricException.class,
        () -> Metric.builder("name").setPrefix("~@#$").setLongCounterValueTotal(1).serialize());
  }

  @Test
  public void testInvalidPrefixAndName() {
    assertThrows(
        MetricException.class,
        () -> Metric.builder("~@#$").setPrefix("!@#").setLongCounterValueTotal(1).serialize());
  }

  @Test
  public void testSetTimestamp() throws MetricException {
    String expected = "prefix.name count,1 1616580000123";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setTimestamp(Instant.ofEpochMilli(1616580000123L))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetInvalidTimestampsSeconds() throws MetricException {
    // timestamp specified in seconds
    String expected = "prefix.name count,1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setTimestamp(Instant.ofEpochMilli(1616580000L))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetInvalidTimestampsNanoseconds() throws MetricException {
    // timestamp specified in nanoseconds
    String expected = "prefix.name count,1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setTimestamp(Instant.ofEpochMilli(1616580000000000L))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testCurrentTimestamp() throws MetricException {
    String expectedStart = "prefix.name count,1 ";
    // this is just for the string length.
    String expectedDummy = "prefix.name count,1 1616580000000";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setCurrentTime()
            .serialize();

    assertTrue(actual.startsWith(expectedStart));
    assertTrue(actual.length() >= expectedDummy.length());
  }

  @Test
  public void testSetDimensions() throws MetricException {
    String expected = "prefix.name,dim1=val1 count,1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setDimensions(DimensionList.create(Dimension.create("dim1", "val1")))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testJustDefaultDimensions() throws MetricException {
    String expected = "prefix.name,defdim1=val1 count,1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setDefaultDimensions(DimensionList.create(Dimension.create("defdim1", "val1")))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetDefaultAndDynamicDimensions() throws MetricException {
    String expectedBase = "prefix.name count,1";
    List<String> expectedDims = Arrays.asList("defaultdim1=defaultVal1", "dim1=val1");
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            // only available in package.
            .setDimensions(DimensionList.create(Dimension.create("dim1", "val1")))
            .setDefaultDimensions(
                DimensionList.create(Dimension.create("defaultDim1", "defaultVal1")))
            .serialize();
    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @Test
  public void testSetDefaultAndDynamicAndOneAgentDimensions() throws MetricException {
    String expectedBase = "prefix.name count,1";
    List<String> expectedDims =
        Arrays.asList("defaultdim1=defaultVal1", "dim1=val1", "oneagentdim1=oneAgentVal1");
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setDimensions(DimensionList.create(Dimension.create("dim1", "val1")))
            .setDefaultDimensions(
                DimensionList.create(Dimension.create("defaultDim1", "defaultVal1")))
            // only available in package.
            .setOneAgentDimensions(
                DimensionList.create(Dimension.create("oneAgentDim1", "oneAgentVal1")))
            .serialize();
    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @Test
  public void testOverwriting() throws MetricException {
    String expectedBase = "prefix.name count,1";
    List<String> expectedDims =
        Arrays.asList("dim1=defaultVal1", "dim2=dynamicVal2", "dim3=oneAgentVal3");
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setDefaultDimensions(
                DimensionList.create(
                    Dimension.create("dim1", "defaultVal1"),
                    Dimension.create("dim2", "defaultVal2"),
                    Dimension.create("dim3", "defaultVal3")))
            .setDimensions(
                DimensionList.create(
                    Dimension.create("dim2", "dynamicVal2"),
                    Dimension.create("dim3", "dynamicVal3")))
            .setOneAgentDimensions(DimensionList.create(Dimension.create("dim3", "oneAgentVal3")))
            .serialize();
    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @Test
  public void testThrowsOnLineTooLong() {
    int numDimensions = 250;
    List<Dimension> dimensions = new ArrayList<>(numDimensions);
    for (int i = 0; i < numDimensions; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.add(Dimension.create(key, val));
    }

    MetricException me =
        assertThrows(
            MetricException.class,
            () ->
                Metric.builder("name")
                    .setPrefix("prefix")
                    .setLongCounterValueTotal(1)
                    .setDefaultDimensions(DimensionList.fromCollection(dimensions))
                    .serialize());

    String expectedMessage =
        "Serialized line exceeds limit of 2000 characters accepted by the ingest API:";

    // the serialized method will have the dimensions in a shuffled manner (due to the duplicate
    // elimination using a map), so we assert that certain dimensions are included, but not the
    // order.
    assertTrue(me.getMessage().contains(expectedMessage));
    assertTrue(me.getMessage().contains("dim0=val0"));
    assertTrue(
        me.getMessage()
            .contains(String.format("dim%d=val%d", numDimensions / 2, numDimensions / 2)));
    assertTrue(
        me.getMessage()
            .contains(String.format("dim%d=val%d", numDimensions - 1, numDimensions - 1)));
  }
}
