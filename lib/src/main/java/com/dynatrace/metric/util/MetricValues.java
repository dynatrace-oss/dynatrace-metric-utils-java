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

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/** Interface type that all Metric values have to follow. */
interface IMetricValue {
  String serialize();
}

public class MetricValues {
  static final class IntCounterValue implements IMetricValue {
    private final int value;
    private final boolean absolute;

    public IntCounterValue(int value, boolean absolute) throws MetricException {
      if (!absolute && value < 0) {
        throw new MetricException("counter value cannot be smaller than 0");
      }
      this.value = value;
      this.absolute = absolute;
    }

    @Override
    public String serialize() {
      if (this.absolute) {
        return String.format("count,delta=%d", this.value);
      }
      return String.format("count,%d", this.value);
    }
  }

  static final class IntSummaryValue implements IMetricValue {
    private final int min;
    private final int max;
    private final int sum;
    private final int count;

    public IntSummaryValue(int min, int max, int sum, int count) throws MetricException {
      if (count < 0) {
        throw new MetricException("count cannot be negative");
      }
      if (min > max) {
        throw new MetricException("min cannot be greater than max!");
      }
      if (max > sum) {
        throw new MetricException("min and max cannot be bigger than the sum");
      }
      this.min = min;
      this.max = max;
      this.sum = sum;
      this.count = count;
    }

    @Override
    public String serialize() {
      return String.format("gauge,min=%d,max=%d,sum=%d,count=%d", min, max, sum, count);
    }
  }

  static final class IntGaugeValue implements IMetricValue {
    private final int value;

    public IntGaugeValue(int value) {
      this.value = value;
    }

    @Override
    public String serialize() {
      return String.format("gauge,%d", value);
    }
  }

  static final class DoubleCounterValue implements IMetricValue {
    private final double value;
    private final boolean absolute;

    public DoubleCounterValue(double value, boolean absolute) throws MetricException {
      if (!absolute && value < 0) {
        throw new MetricException("counter value cannot be smaller than 0");
      }
      this.value = value;
      this.absolute = absolute;
    }

    @Override
    public String serialize() {
      if (this.absolute) {
        return String.format("count,delta=%s", formatDouble(this.value));
      }
      return String.format("count,%s", formatDouble(this.value));
    }
  }

  static final class DoubleSummaryValue implements IMetricValue {
    private final double min;
    private final double max;
    private final double sum;
    private final int count;

    public DoubleSummaryValue(double min, double max, double sum, int count)
        throws MetricException {
      if (count < 0) {
        throw new MetricException("count cannot be negative");
      }
      if (min > max) {
        throw new MetricException("min cannot be greater than max!");
      }
      if (max > sum) {
        throw new MetricException("min and max cannot be bigger than the sum");
      }

      this.min = min;
      this.max = max;
      this.sum = sum;
      this.count = count;
    }

    @Override
    public String serialize() {
      return String.format(
          "gauge,min=%s,max=%s,sum=%s,count=%d",
          formatDouble(min), formatDouble(max), formatDouble(sum), count);
    }
  }

  static final class DoubleGaugeValue implements IMetricValue {
    private final double value;

    public DoubleGaugeValue(double value) {
      this.value = value;
    }

    @Override
    public String serialize() {
      return String.format("gauge,%s", formatDouble(value));
    }
  }

  static String formatDouble(double d) {
    String formatted = String.format("%.6f", d);
    // trim trailing zeros and dots
    formatted = CharMatcher.anyOf(".0").trimTrailingFrom(formatted);
    if (Strings.isNullOrEmpty(formatted)) {
      // everything was trimmed away, number was 0.0000
      return "0";
    }
    return formatted;
  }
}