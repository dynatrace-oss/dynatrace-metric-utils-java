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
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MetricBuilderTest {
  private void assertListsEqualIgnoreOrder(List<String> expected, List<String> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
  }

  @Test
  @Deprecated
  void testSetLongCounterValueTotal() throws MetricException {
    String expected = "name count,1";
    String actual = Metric.builder("name").setLongCounterValueTotal(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetLongGaugeValue() throws MetricException {
    String expected = "name gauge,1";
    String actual = Metric.builder("name").setLongGaugeValue(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetLongCounterValueDelta() throws MetricException {
    String expected = "name count,delta=1";
    String actual = Metric.builder("name").setLongCounterValueDelta(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetLongSummaryValue() throws MetricException {
    String expected = "name gauge,min=1,max=10,sum=20,count=7";
    String actual = Metric.builder("name").setLongSummaryValue(1, 10, 20, 7).serialize();

    assertEquals(expected, actual);
  }

  @Test
  @Deprecated
  void testSetDoubleCounterValueTotal() throws MetricException {
    String expected = "name count,1.23";
    String actual = Metric.builder("name").setDoubleCounterValueTotal(1.23).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDoubleGauge() throws MetricException {
    String expected = "name gauge,1.23";
    String actual = Metric.builder("name").setDoubleGaugeValue(1.23).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDoubleCounterValueDelta() throws MetricException {
    String expected = "name count,delta=1.23";
    String actual = Metric.builder("name").setDoubleCounterValueDelta(1.23).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDoubleSummaryValue() throws MetricException {
    String expected = "name gauge,min=1.23,max=10.34,sum=20.45,count=7";
    String actual = Metric.builder("name").setDoubleSummaryValue(1.23, 10.34, 20.45, 7).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testNoValueSet() {
    assertThrows(MetricException.class, () -> Metric.builder("name").serialize());
  }

  @Test
  void testValueSetTwice() {
    // first setting of value does not throw.
    Metric.Builder builder =
        assertDoesNotThrow(() -> Metric.builder("name").setLongCounterValueDelta(1));

    // trying to set again throws an exception.
    assertThrows(MetricException.class, () -> builder.setDoubleCounterValueDelta(1.23));
  }

  @Test
  void testNameContainsInvalidChars() throws MetricException {
    String expected = "_ count,delta=1";
    String actual = Metric.builder("~@#$").setLongCounterValueDelta(1).serialize();
    assertEquals(expected, actual);
  }

  @Test
  void testPrefix() throws MetricException {
    String expected = "prefix.name count,delta=1";
    String actual =
        Metric.builder("name").setPrefix("prefix").setLongCounterValueDelta(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testPrefixWithTrailingDot() throws MetricException {
    String expected = "prefix.name count,delta=1";
    String actual =
        Metric.builder("name").setPrefix("prefix.").setLongCounterValueDelta(1).serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testPrefixContainsInvalidChars() throws MetricException {
    String expected = "_.name count,delta=1";
    String actual =
        Metric.builder("name").setPrefix("~@#$").setLongCounterValueDelta(1).serialize();
    assertEquals(expected, actual);
  }

  @Test
  void testPrefixAndNameContainInvalidChars() throws MetricException {
    String expected = "_._ count,delta=1";
    String actual = Metric.builder("~@#$").setPrefix("!@#").setLongCounterValueDelta(1).serialize();
    assertEquals(expected, actual);
  }

  @Test
  void testSetTimestamp() throws MetricException {
    String expected = "prefix.name count,delta=1 1616580000123";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setTimestamp(Instant.ofEpochMilli(1616580000123L))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetInvalidTimestampsSeconds() throws MetricException {
    // timestamp specified in seconds
    String expected = "prefix.name count,delta=1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setTimestamp(Instant.ofEpochMilli(1616580000L))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetInvalidTimestampsNanoseconds() throws MetricException {
    // timestamp specified in nanoseconds
    String expected = "prefix.name count,delta=1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setTimestamp(Instant.ofEpochMilli(1616580000000000L))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testCurrentTimestamp() throws MetricException {
    String expectedStart = "prefix.name count,delta=1 ";
    // this is just for the string length.
    String expectedDummy = "prefix.name count,delta=1 1616580000000";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setCurrentTime()
            .serialize();

    assertTrue(actual.startsWith(expectedStart));
    assertTrue(actual.length() >= expectedDummy.length());
  }

  @Test
  void testSetDimensions() throws MetricException {
    String expected = "prefix.name,dim1=val1 count,delta=1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setDimensions(DimensionList.create(Dimension.create("dim1", "val1")))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testJustDefaultDimensions() throws MetricException {
    String expected = "prefix.name,defdim1=val1 count,delta=1";
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setDefaultDimensions(DimensionList.create(Dimension.create("defdim1", "val1")))
            .serialize();

    assertEquals(expected, actual);
  }

  @Test
  void testSetDefaultAndDynamicDimensions() throws MetricException {
    String expectedBase = "prefix.name count,delta=1";
    List<String> expectedDims = Arrays.asList("defaultdim1=defaultVal1", "dim1=val1");
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
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
  void testSetDefaultAndDynamicAndMetadataDimensions() throws MetricException {
    String expectedBase = "prefix.name count,delta=1";
    List<String> expectedDims =
        Arrays.asList("defaultdim1=defaultVal1", "dim1=val1", "metadatadim1=metadataVal1");
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setDimensions(DimensionList.create(Dimension.create("dim1", "val1")))
            .setDefaultDimensions(
                DimensionList.create(Dimension.create("defaultDim1", "defaultVal1")))
            // only available in package.
            .setDynatraceMetadataDimensions(
                DimensionList.create(Dimension.create("metadataDim1", "metadataVal1")))
            .serialize();
    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @Test
  void testOverwriting() throws MetricException {
    String expectedBase = "prefix.name count,delta=1";
    List<String> expectedDims =
        Arrays.asList("dim1=defaultVal1", "dim2=dynamicVal2", "dim3=metadataVal3");
    String actual =
        Metric.builder("name")
            .setPrefix("prefix")
            .setLongCounterValueDelta(1)
            .setDefaultDimensions(
                DimensionList.create(
                    Dimension.create("dim1", "defaultVal1"),
                    Dimension.create("dim2", "defaultVal2"),
                    Dimension.create("dim3", "defaultVal3")))
            .setDimensions(
                DimensionList.create(
                    Dimension.create("dim2", "dynamicVal2"),
                    Dimension.create("dim3", "dynamicVal3")))
            .setDynatraceMetadataDimensions(
                DimensionList.create(Dimension.create("dim3", "metadataVal3")))
            .serialize();
    String actualBase = actual.split(",", 2)[0] + " " + actual.split(" ")[1];
    List<String> actualDims = Arrays.asList(actual.split(",", 2)[1].split(" ")[0].split(","));

    assertEquals(expectedBase, actualBase);
    assertListsEqualIgnoreOrder(expectedDims, actualDims);
  }

  @Test
  void testThrowsOnLineTooLong() throws MetricException {
    // shortest dimension/value pair: 'dim0=val0' (9 chars); max line length: 50_000 characters
    int numDimensions = 50_000 / 9;
    List<Dimension> dimensions = new ArrayList<>(numDimensions);
    for (int i = 0; i < numDimensions; i++) {
      String key = String.format("dim%d", i);
      String val = String.format("val%d", i);
      dimensions.add(Dimension.create(key, val));
    }
    Metric.Builder metricBuilder =
        Metric.builder("name")
            .setPrefix("prefix")
            .setDoubleCounterValueDelta(1)
            .setDefaultDimensions(DimensionList.fromCollection(dimensions));

    MetricException me = assertThrows(MetricException.class, metricBuilder::serialize);

    assertTrue(
        me.getMessage()
            .startsWith(
                "Serialized line exceeds limit of 50000 characters accepted by the ingest API:"));
    // The message is truncated (to not print 50.000 characters) and the dimensions are in a random
    // order (due to the deduplication, which uses a map that does not preserve order). Therefore,
    // we can just assert that there are some dimensions before the truncation, but not which ones.
    Pattern dimensionPattern = Pattern.compile("dim\\d+=val\\d+,");
    assertTrue(dimensionPattern.matcher(me.getMessage()).find());

    assertTrue(me.getMessage().endsWith("... (truncated)"));
  }
}
