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

public final class Metric {
  public static final class Builder {
    private final String name;
    private String prefix;
    private IMetricValue value;
    private Instant time;
    private DimensionList dimensions;
    private DimensionList defaultDimensions;
    private DimensionList oneAgentDimensions;

    private Builder(String name) {
      this.name = name;
    }

    Builder setDefaultDimensions(DimensionList defaultDimensions) {
      this.defaultDimensions = defaultDimensions;
      return this;
    }

    Builder setOneAgentDimensions(DimensionList oneAgentDimensions) {
      this.oneAgentDimensions = oneAgentDimensions;
      return this;
    }

    private void throwIfValueAlreadySet() throws MetricException {
      if (this.value != null) {
        throw new MetricException("Value already set.");
      }
    }

    /**
     * (Optional) Set the prefix on the builder object.
     *
     * @param prefix to be prepended to the name
     * @return this
     */
    public Builder setPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Use an int counter value for the current metric. Will produce the entry "count,<number>" in
     * the resulting metric line.
     *
     * @param value the value to be serialized.
     * @return this
     */
    public Builder setIntCounterValue(int value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.IntCounterValue(value, false);
      return this;
    }

    /**
     * Use an int absolute counter value for the current metric. Will produce the entry
     * "count,delta=<number>" in the resulting metric line.
     *
     * @param value the value to be serialized
     * @return this
     */
    public Builder setIntAbsoluteCounterValue(int value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.IntCounterValue(value, true);
      return this;
    }

    /**
     * Use an int gauge value for the current metric. Will produce the entry "gauge,<number>" in the
     * resulting metric line
     *
     * @param value the value to be serialized
     * @return this
     */
    public Builder setIntGaugeValue(int value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.IntGaugeValue(value);
      return this;
    }

    /**
     * Use an int summary value for the current metric. Will produce the entry
     * "gauge,min=<min>,max=<max>,sum=<sum>,count=<count>" in the resulting metric line
     *
     * @param min the minimum value for the current metric.
     * @param max the maximum value for the current metric.
     * @param sum the sum of all values in the recorded timeframe
     * @param count the number of elements contributing to the value.
     * @return this.
     * @throws MetricException if min or max are greater than the sum or if the count is negative
     */
    public Builder setIntSummaryValue(int min, int max, int sum, int count) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.IntSummaryValue(min, max, sum, count);
      return this;
    }

    /**
     * Use an double counter value for the current metric. Will produce the entry "count,<number>"
     * in the resulting metric line.
     *
     * @param value the value to be serialized.
     * @return this
     */
    public Builder setDoubleCounterValue(double value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleCounterValue(value, false);
      return this;
    }

    /**
     * Use an double absolute counter value for the current metric. Will produce the entry
     * "count,delta=<number>" in the resulting metric line.
     *
     * @param value the value to be serialized
     * @return this
     */
    public Builder setDoubleAbsoluteCounterValue(double value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleCounterValue(value, true);
      return this;
    }

    /**
     * Use an double gauge value for the current metric. Will produce the entry "gauge,<number>" in
     * the resulting metric line
     *
     * @param value the value to be serialized
     * @return this
     */
    public Builder setDoubleGaugeValue(double value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleGaugeValue(value);
      return this;
    }

    /**
     * Use an double summary value for the current metric. Will produce the entry
     * "gauge,min=<min>,max=<max>,sum=<sum>,count=<count>" in the resulting metric line
     *
     * @param min the minimum value for the current metric.
     * @param max the maximum value for the current metric.
     * @param sum the sum of all values in the recorded timeframe
     * @param count the number of elements contributing to the value.
     * @return this.
     * @throws MetricException if min or max are greater than the sum or if the count is negative
     */
    public Builder setDoubleSummaryValue(double min, double max, double sum, int count)
        throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleSummaryValue(min, max, sum, count);
      return this;
    }

    /**
     * (Optional) Set the {@link DimensionList} to be serialized. Can be obtained from {@link
     * DimensionList#merge the merge function} if multiple should be passed, or a single list can be
     * added. When called multiple times, this will overwrite previously added dimensions. If the
     * builder was created by the metric builder factory, default and OneAgent dimensions will be
     * added to the passed dimensions.
     *
     * @param dimensions the {@link DimensionList} to be serialized
     * @return this
     */
    public Builder setDimensions(DimensionList dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    /**
     * (Optional) Set the timestamp for the exported metric line. In most cases, {@link
     * Builder#setCurrentTime()} should be suitable.
     *
     * @param timestamp an {@link Instant} object describing the time at which the {@link Metric}
     *     was created.
     * @return this
     */
    public Builder setTimestamp(Instant timestamp) {
      this.time = timestamp;
      return this;
    }

    /**
     * (Optional) Calls {@link Builder#setTimestamp} with the current time.
     *
     * @return this
     */
    public Builder setCurrentTime() {
      return this.setTimestamp(Instant.now());
    }

    /**
     * Serialize a {@link Metric.Builder} object to a String in a valid format for the Dynatrace
     * metrics ingest endpoint.
     *
     * @return A {@link String} containing all properties set on the {@link Metric.Builder} in an
     *     ingestion-ready format.
     * @throws MetricException If no value is set or if the prefix/name combination evaluates to an
     *     invalid/empty key after normalization.
     */
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

      // combine default dimensions, dynamic dimensions and OneAgent dimensions into one list.
      DimensionList allDimensions =
          DimensionList.merge(this.defaultDimensions, this.dimensions, this.oneAgentDimensions);
      String dimensionsString = null;
      if (!allDimensions.isEmpty()) {
        dimensionsString = allDimensions.serialize();
      }

      // if any dimensions are present, serialize and append them to the metric string.
      if (!Strings.isNullOrEmpty(dimensionsString)) {
        builder.append(",");
        builder.append(dimensionsString);
      }
      builder.append(" ");

      // add the serialized value to the metric string.
      builder.append(this.value.serialize());

      // if a timestamp is set, add it to the metric string.
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

  /**
   * Create a new {@link Builder Metric.Builder} object.
   *
   * @param name The metric name.
   * @return A new {@link Builder} object with the name property set.
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }
}
