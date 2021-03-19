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

import org.junit.jupiter.api.Test;

class MetricValuesTest {
  @Test
  public void testFormatDouble() {
    assertEquals("0", MetricValues.formatDouble(0));
    assertEquals("0", MetricValues.formatDouble(-0));
    assertEquals("0", MetricValues.formatDouble(.000000000000000));
    assertEquals("1", MetricValues.formatDouble(1));
    assertEquals("1", MetricValues.formatDouble(1.00000000000000));
    assertEquals("1.234567", MetricValues.formatDouble(1.234567));
    assertEquals("1.234568", MetricValues.formatDouble(1.234567890));
    assertEquals("-1.234567", MetricValues.formatDouble(-1.234567));
    assertEquals("-1.234568", MetricValues.formatDouble(-1.234567890));
  }

  @Test
  public void testIntCounterValue() throws MetricException {
    MetricValues.IntCounterValue val;
    val = new MetricValues.IntCounterValue(0, false);
    assertEquals("count,0", val.serialize());

    val = new MetricValues.IntCounterValue(100, false);
    assertEquals("count,100", val.serialize());
    
    assertThrows(MetricException.class, () -> new MetricValues.IntCounterValue(-10, false));

    val = new MetricValues.IntCounterValue(0, true);
    assertEquals("count,delta=0", val.serialize());

    val = new MetricValues.IntCounterValue(100, true);
    assertEquals("count,delta=100", val.serialize());

    val = new MetricValues.IntCounterValue(-10, true);
    assertEquals("count,delta=-10", val.serialize());
  }
  
  @Test
  public void testIntSummaryValue() throws MetricException {
    MetricValues.IntSummaryValue val;
    val = new MetricValues.IntSummaryValue(0, 10, 20, 10);
    assertEquals("gauge,min=0,max=10,sum=20,count=10", val.serialize());

    val = new MetricValues.IntSummaryValue(0, 0, 0, 0);
    assertEquals("gauge,min=0,max=0,sum=0,count=0", val.serialize());

    assertThrows(MetricException.class, () -> new MetricValues.IntSummaryValue(0, 10, 20, -10));
    assertThrows(MetricException.class, () -> new MetricValues.IntSummaryValue(5, 3, 20, 10));
    assertThrows(MetricException.class, () -> new MetricValues.IntSummaryValue(100, 100, 20, 10));
  }

  @Test
  public void testIntGaugeValue() {
    MetricValues.IntGaugeValue val;
    val = new MetricValues.IntGaugeValue(0);
    assertEquals("gauge,0", val.serialize());

    val = new MetricValues.IntGaugeValue(123);
    assertEquals("gauge,123", val.serialize());

    val = new MetricValues.IntGaugeValue(-123);
    assertEquals("gauge,-123", val.serialize());
  }

  @Test
  public void testDoubleCounterValue() throws MetricException {
    MetricValues.DoubleCounterValue val;
    val = new MetricValues.DoubleCounterValue(0.000, false);
    assertEquals("count,0", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123, false);
    assertEquals("count,100.123", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123456789, false);
    assertEquals("count,100.123457", val.serialize());

    assertThrows(MetricException.class, () -> new MetricValues.DoubleCounterValue(-10.123, false));

    val = new MetricValues.DoubleCounterValue(0.000, true);
    assertEquals("count,delta=0", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123, true);
    assertEquals("count,delta=100.123", val.serialize());

    val = new MetricValues.DoubleCounterValue(100.123456789, true);
    assertEquals("count,delta=100.123457", val.serialize());

    val = new MetricValues.DoubleCounterValue(-10.123, true);
    assertEquals("count,delta=-10.123", val.serialize());
  }

  @Test
  public void testDoubleSummaryValue() throws MetricException {
    MetricValues.DoubleSummaryValue val;
    val = new MetricValues.DoubleSummaryValue(0.123, 10.321, 20.456, 10);
    assertEquals("gauge,min=0.123,max=10.321,sum=20.456,count=10", val.serialize());

    val = new MetricValues.DoubleSummaryValue(0.000, 0.000, 0.000, 0);
    assertEquals("gauge,min=0,max=0,sum=0,count=0", val.serialize());

    // negative count
    assertThrows(MetricException.class, () -> new MetricValues.DoubleSummaryValue(0.2, 10.3, 20.4, -10));
    // min > max
    assertThrows(MetricException.class, () -> new MetricValues.DoubleSummaryValue(5.3, 3.3, 20.3, 10));
    // max > sum
    assertThrows(MetricException.class, () -> new MetricValues.DoubleSummaryValue(1.2, 100.4, 20.3, 10));
  }

  @Test
  public void testDoubleGaugeValue() {
    MetricValues.DoubleGaugeValue val;
    val = new MetricValues.DoubleGaugeValue(0.000);
    assertEquals("gauge,0", val.serialize());

    val = new MetricValues.DoubleGaugeValue(123.456);
    assertEquals("gauge,123.456", val.serialize());

    val = new MetricValues.DoubleGaugeValue(-123.456);
    assertEquals("gauge,-123.456", val.serialize());
  }
  
}
