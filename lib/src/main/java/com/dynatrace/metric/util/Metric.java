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

import java.time.LocalDateTime;

public class Metric {

  public static class Builder {
    private String name;
    private String prefix;
    private MetricValue value;
    private DimensionList dimensions;
    private LocalDateTime time;

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

    public Builder setTimestamp(LocalDateTime timestamp) {
      this.time = timestamp;
      return this;
    }

    public Builder setCurrentTime() {
      return this.setTimestamp(LocalDateTime.now());
    }

    public Metric build() {
      // todo run merge here.
      return new Metric(prefix, name, value, dimensions, time);
    }
  }

  private String prefix;
  private String name;
  private MetricValue value;
  private DimensionList dimensions;
  private LocalDateTime time;

  private Metric(
      String prefix, String name, MetricValue value, DimensionList dimensions, LocalDateTime time) {
    this.prefix = prefix;
    this.name = name;
    this.value = value;
    this.dimensions = dimensions;
    this.time = time;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public String serialize() {
    // todo
    return String.format("%s.%s dims %s time", prefix, name, value.serialize());
  }
}
