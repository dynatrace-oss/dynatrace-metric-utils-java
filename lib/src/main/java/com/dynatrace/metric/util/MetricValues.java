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

/**
 * Interface type that all Metric values have to follow.
 */
interface IMetricValue {
  String serialize();
}

public class MetricValues {
  public static class IntCounterValue implements IMetricValue {
    private final int value;
    private final boolean absolute;

    public IntCounterValue(int value) {
      this(value, false);
    }

    public IntCounterValue(int value, boolean absolute) {
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
}
