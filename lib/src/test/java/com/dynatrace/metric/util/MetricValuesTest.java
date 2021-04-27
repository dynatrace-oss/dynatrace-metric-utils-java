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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricValuesTest {
  @Test
  public void testFormatDouble() {
    assertEquals("0.0", MetricValues.formatDouble(0));
    assertEquals("0.0", MetricValues.formatDouble(-0));
    assertEquals("0.0", MetricValues.formatDouble(.000000000000000));
    assertEquals("1.0", MetricValues.formatDouble(1));
    assertEquals("1.0", MetricValues.formatDouble(1.00000000000000));
    assertEquals("1.23456789", MetricValues.formatDouble(1.23456789000));
    assertEquals("1.1234567890123457", MetricValues.formatDouble(1.1234567890123456789));
    assertEquals("-1.23456789", MetricValues.formatDouble(-1.23456789000));
    assertEquals("-1.1234567890123457", MetricValues.formatDouble(-1.1234567890123456789));
    assertEquals("200.0", MetricValues.formatDouble(200));
    assertEquals("200.0", MetricValues.formatDouble(200.00000000));
    assertEquals("1.0E10", MetricValues.formatDouble(1e10));
    assertEquals("1.0E12", MetricValues.formatDouble(1_000_000_000_000d));
    assertEquals("1.234567E12", MetricValues.formatDouble(1_234_567_000_000d));
    assertEquals("1.234567000000123E12", MetricValues.formatDouble(1_234_567_000_000.123));
    assertEquals("1.7976931348623157E308", MetricValues.formatDouble(Double.MAX_VALUE));
    assertEquals("4.9E-324", MetricValues.formatDouble(Double.MIN_VALUE));
    // these should never happen in the execution of the program, but these tests ensure that
    // nothing is thrown from the the format method.
    assertEquals("NaN", MetricValues.formatDouble(Double.NaN));
    assertEquals("Infinity", MetricValues.formatDouble(Double.POSITIVE_INFINITY));
    assertEquals("-Infinity", MetricValues.formatDouble(Double.NEGATIVE_INFINITY));
  }

  @Test
  public void testLongCounterValue() throws MetricException {
    MetricValues.LongCounterValue val;
    val = new MetricValues.LongCounterValue(0, false);
    assertEquals("count,0", val.serialize());

    val = new MetricValues.LongCounterValue(100, false);
    assertEquals("count,100", val.serialize());

    assertThrows(MetricException.class, () -> new MetricValues.LongCounterValue(-10, false));

    val = new MetricValues.LongCounterValue(0, true);
    assertEquals("count,delta=0", val.serialize());

    val = new MetricValues.LongCounterValue(100, true);
    assertEquals("count,delta=100", val.serialize());

    val = new MetricValues.LongCounterValue(-10, true);
    assertEquals("count,delta=-10", val.serialize());
  }

  @Test
  public void testLongSummaryValue() throws MetricException {
    MetricValues.LongSummaryValue val;
    val = new MetricValues.LongSummaryValue(0, 10, 20, 10);
    assertEquals("gauge,min=0,max=10,sum=20,count=10", val.serialize());

    val = new MetricValues.LongSummaryValue(0, 0, 0, 0);
    assertEquals("gauge,min=0,max=0,sum=0,count=0", val.serialize());

    assertThrows(MetricException.class, () -> new MetricValues.LongSummaryValue(0, 10, 20, -10));
    assertThrows(MetricException.class, () -> new MetricValues.LongSummaryValue(5, 3, 20, 10));
  }

  @Test
  public void testLongGaugeValue() {
    MetricValues.LongGaugeValue val;
    val = new MetricValues.LongGaugeValue(0);
    assertEquals("gauge,0", val.serialize());

    val = new MetricValues.LongGaugeValue(123);
    assertEquals("gauge,123", val.serialize());

    val = new MetricValues.LongGaugeValue(-123);
    assertEquals("gauge,-123", val.serialize());
  }

  @Test
  public void testDoubleCounterValue() throws MetricException {
    MetricValues.DoubleCounterValue val;
    val = new MetricValues.DoubleCounterValue(0.000, false);
    assertEquals("count,0.0", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123, false);
    assertEquals("count,100.123", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123456789, false);
    assertEquals("count,100.123456789", val.serialize());

    assertThrows(MetricException.class, () -> new MetricValues.DoubleCounterValue(-10.123, false));

    val = new MetricValues.DoubleCounterValue(0.000, true);
    assertEquals("count,delta=0.0", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123, true);
    assertEquals("count,delta=100.123", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123456789, true);
    assertEquals("count,delta=100.123456789", val.serialize());

    val = new MetricValues.DoubleCounterValue(-10.123, true);
    assertEquals("count,delta=-10.123", val.serialize());

    assertThrows(
        MetricException.class, () -> new MetricValues.DoubleCounterValue(Double.NaN, false));
    assertThrows(
        MetricException.class,
        () -> new MetricValues.DoubleCounterValue(Double.NEGATIVE_INFINITY, false));
    assertThrows(
        MetricException.class,
        () -> new MetricValues.DoubleCounterValue(Double.POSITIVE_INFINITY, false));
    assertThrows(
        MetricException.class, () -> new MetricValues.DoubleCounterValue(Double.NaN, true));
    assertThrows(
        MetricException.class,
        () -> new MetricValues.DoubleCounterValue(Double.NEGATIVE_INFINITY, true));
    assertThrows(
        MetricException.class,
        () -> new MetricValues.DoubleCounterValue(Double.POSITIVE_INFINITY, true));
  }

  @Test
  public void testDoubleSummaryValue() throws MetricException {
    MetricValues.DoubleSummaryValue val;
    val = new MetricValues.DoubleSummaryValue(0.123, 10.321, 20.456, 10);
    assertEquals("gauge,min=0.123,max=10.321,sum=20.456,count=10", val.serialize());

    val = new MetricValues.DoubleSummaryValue(0.000, 0.000, 0.000, 0);
    assertEquals("gauge,min=0.0,max=0.0,sum=0.0,count=0", val.serialize());

    // negative count
    assertThrows(
        MetricException.class, () -> new MetricValues.DoubleSummaryValue(0.2, 10.3, 20.4, -10));
    // min > max
    assertThrows(
        MetricException.class, () -> new MetricValues.DoubleSummaryValue(5.3, 3.3, 20.3, 10));

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
              () -> new MetricValues.DoubleSummaryValue(minVal, maxVal, sumVal, 1));
        }
      }
    }
  }

  @Test
  public void testDoubleGaugeValue() throws MetricException {
    MetricValues.DoubleGaugeValue val;
    val = new MetricValues.DoubleGaugeValue(0.000);
    assertEquals("gauge,0.0", val.serialize());

    val = new MetricValues.DoubleGaugeValue(123.456);
    assertEquals("gauge,123.456", val.serialize());

    val = new MetricValues.DoubleGaugeValue(-123.456);
    assertEquals("gauge,-123.456", val.serialize());

    assertThrows(MetricException.class, () -> new MetricValues.DoubleGaugeValue(Double.NaN));
    assertThrows(
        MetricException.class, () -> new MetricValues.DoubleGaugeValue(Double.NEGATIVE_INFINITY));
    assertThrows(
        MetricException.class, () -> new MetricValues.DoubleGaugeValue(Double.POSITIVE_INFINITY));
  }
}
