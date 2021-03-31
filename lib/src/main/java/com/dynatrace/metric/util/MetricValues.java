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

import java.util.regex.Pattern;

/** Interface type that all Metric values have to follow. */
interface IMetricValue {
  String serialize();
}

/** Holder class for the different value classes. */
final class MetricValues {
  static final class LongCounterValue implements IMetricValue {
    private final long value;
    private final boolean isDelta;

    public LongCounterValue(long value, boolean isDelta) throws MetricException {
      if (!isDelta && value < 0) {
        throw new MetricException("counter value cannot be smaller than 0");
      }
      this.value = value;
      this.isDelta = isDelta;
    }

    @Override
    public String serialize() {
      if (this.isDelta) {
        return String.format("count,delta=%d", this.value);
      }
      return String.format("count,%d", this.value);
    }
  }

  static final class LongSummaryValue implements IMetricValue {
    private final long min;
    private final long max;
    private final long sum;
    private final long count;

    public LongSummaryValue(long min, long max, long sum, long count) throws MetricException {
      if (count < 0) {
        throw new MetricException("count cannot be negative");
      }
      if (min > max) {
        throw new MetricException("min cannot be greater than max!");
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

  static final class LongGaugeValue implements IMetricValue {
    private final long value;

    public LongGaugeValue(long value) {
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

    public DoubleCounterValue(double value, boolean isDelta) throws MetricException {
      if (!isDelta && value < 0) {
        throw new MetricException("counter value cannot be smaller than 0");
      }
      this.value = value;
      this.absolute = isDelta;
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
    private final long count;

    public DoubleSummaryValue(double min, double max, double sum, long count)
        throws MetricException {
      if (count < 0) {
        throw new MetricException("count cannot be negative");
      }
      if (min > max) {
        throw new MetricException("min cannot be greater than max!");
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

  private static final Pattern regexTrailingZeroesOrDots = Pattern.compile("[0.]+$");

  static String formatDouble(double d) {
    String formatted = String.format("%.6f", d);
    // trim trailing zeros and dots
    formatted = regexTrailingZeroesOrDots.matcher(formatted).replaceAll("");
    if (formatted.isEmpty()) {
      // everything was trimmed away, number was 0.0000
      return "0";
    }
    return formatted;
  }
}
