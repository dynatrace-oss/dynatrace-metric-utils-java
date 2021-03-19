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

import com.google.common.base.Strings;
import java.time.Instant;

public class Metric {

  public static class Builder {
    private final String name;
    private String prefix;
    private IMetricValue value;
    private DimensionList dimensions;
    private Instant time;

    private Builder(String name) {
      this.name = name;
    }

    public Builder setPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public Builder setIntCounterValue(int value) {
      this.value = new MetricValues.IntCounterValue(value);
      return this;
    }

    public Builder setDimensions(DimensionList dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    public Builder setTimestamp(Instant timestamp) {
      this.time = timestamp;
      return this;
    }

    public Builder setCurrentTime() {
      return this.setTimestamp(Instant.now());
    }

    public Metric build() {
      return new Metric(prefix, name, value, dimensions, time);
    }
  }

  private final String prefix;
  private final String name;
  private final IMetricValue value;
  private final DimensionList dimensions;
  private final Instant time;

  private Metric(
      String prefix, String name, IMetricValue value, DimensionList dimensions, Instant time) {
    this.prefix = prefix;
    this.name = name;
    this.value = value;
    this.dimensions = dimensions;
    this.time = time;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public String serialize() throws MetricException {
    String normalizedKeyString = makeNormalizedMetricName();
    if (Strings.isNullOrEmpty(normalizedKeyString)) {
      throw new MetricException("normalized metric key is empty.");
    }

    if (this.value == null) {
      throw new MetricException("no value set for metric");
    }

    // the two required arguments, name and value, are set and valid, so we start assembling the
    // metric line here.
    StringBuilder builder = new StringBuilder(normalizedKeyString);

    String dimensionsString = null;
    if (this.dimensions != null) {
      dimensionsString = this.dimensions.serialize();
    }

    if (!Strings.isNullOrEmpty(dimensionsString)) {
      builder.append(",");
      builder.append(dimensionsString);
      builder.append(" ");
    }

    builder.append(this.value.serialize());

    if (this.time != null) {
      builder.append(" ");
      builder.append(time.getEpochSecond());
    }

    return builder.toString();
  }

  private String makeNormalizedMetricName() {
    if (this.prefix == null) {
      return Normalize.metricKey(name);
    }

    return Normalize.metricKey(String.format("%s.%s", prefix, name));
  }
}
