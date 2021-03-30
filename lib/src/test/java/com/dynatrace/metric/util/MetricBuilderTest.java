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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricBuilderTest {
  private void assertListsEqualIgnoreOrder(List<String> expected, List<String> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
  }

  @Test
  public void testSetIntCount() throws MetricException {
    String expected = "name count,1";
    String actual = Metric.builder("name").setLongCounterValueTotal(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testIntGauge() throws MetricException {
    String expected = "name gauge,1";
    String actual = Metric.builder("name").setLongGaugeValue(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testIntAbsoluteCounter() throws MetricException {
    String expected = "name count,delta=1";
    String actual = Metric.builder("name").setLongCounterValueDelta(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetIntSummaryValue() throws MetricException {
    String expected = "name gauge,min=1,max=10,sum=20,count=7";
    String actual = Metric.builder("name").setLongSummaryValue(1, 10, 20, 7).serialize();

    assertEquals(expected, actual);
  }

  @Test
  public void testSetDoubleCount() throws MetricException {
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
  public void testSetDoubleAbsoluteCounter() throws MetricException {
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
    String expected = "prefix.name count,1 1616580000";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueTotal(1)
            .setTimestamp(Instant.ofEpochSecond(1616580000))
            .serialize();

    assertEquals(expected, actual);
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
}
