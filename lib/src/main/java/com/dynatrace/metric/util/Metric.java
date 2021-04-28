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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.logging.Logger;

/**
 * Represents a single data point consisting of a metric key (with optional prefix), a value, an
 * optional timestamp and optional dimensions.
 */
public final class Metric {

  /** Builder class for {@link Metric Metrics}. */
  public static final class Builder {
    private static final Logger logger = Logger.getLogger(Builder.class.getName());
    private static short numberOfTimestampWarnings = 0;
    private final String metricKey;
    private String prefix;
    private IMetricValue value;
    private Instant time;
    private DimensionList dimensions;
    private DimensionList defaultDimensions;
    private DimensionList oneAgentDimensions;

    private Builder(String metricKey) {
      this.metricKey = metricKey;
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
        throw new MetricException("A value was already set for this metric.");
      }
    }

    /**
     * (Optional) Set the prefix on the builder object.
     *
     * @param prefix to be prepended to the metric key
     * @return this
     */
    public Builder setPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Use a long counter value for the current metric. Will produce the entry "count,[value]" in
     * the resulting metric line.
     *
     * @param value the value to be serialized. Only positive counter values are accepted.
     * @return this
     * @throws MetricException if the a value has already been set on the metric or if the value is
     *     less than 0.
     */
    public Builder setLongCounterValueTotal(long value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.LongCounterValue(value, false);
      return this;
    }

    /**
     * Use a long absolute counter value for the current metric. Will produce the entry
     * "count,delta=[value]" in the resulting metric line.
     *
     * @param value the value to be serialized
     * @return this
     * @throws MetricException if the a value has already been set on the metric.
     */
    public Builder setLongCounterValueDelta(long value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.LongCounterValue(value, true);
      return this;
    }

    /**
     * Use a long gauge value for the current metric. Will produce the entry "gauge,[value]" in the
     * resulting metric line
     *
     * @param value the value to be serialized
     * @return this
     * @throws MetricException if the a value has already been set on the metric.
     */
    public Builder setLongGaugeValue(long value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.LongGaugeValue(value);
      return this;
    }

    /**
     * Use a long summary value for the current metric. Will produce the entry
     * "gauge,min=[min],max=[max],sum=[sum],count=[count]" in the resulting metric line
     *
     * @param min the minimum value for the current metric.
     * @param max the maximum value for the current metric.
     * @param sum the sum of all values in the recorded timeframe
     * @param count the number of elements contributing to the value.
     * @return this.
     * @throws MetricException if the a value has already been set on the metric. Also thrown if min
     *     or max are greater than the sum or if the count is negative.
     */
    public Builder setLongSummaryValue(long min, long max, long sum, long count)
        throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.LongSummaryValue(min, max, sum, count);
      return this;
    }

    /**
     * Use a double counter value for the current metric. Will produce the entry "count,[value]" in
     * the resulting metric line.
     *
     * @param value the value to be serialized. Only positive counter values are accepted.
     * @return this
     * @throws MetricException if the a value has already been set on the metric or if the value is
     *     less than 0.
     */
    public Builder setDoubleCounterValueTotal(double value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleCounterValue(value, false);
      return this;
    }

    /**
     * Use a double absolute counter value for the current metric. Will produce the entry
     * "count,delta=[value]" in the resulting metric line.
     *
     * @param value the value to be serialized
     * @return this
     * @throws MetricException if the a value has already been set on the metric.
     */
    public Builder setDoubleCounterValueDelta(double value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleCounterValue(value, true);
      return this;
    }

    /**
     * Use a double gauge value for the current metric. Will produce the entry "gauge,[value]" in
     * the resulting metric line
     *
     * @param value the value to be serialized
     * @return this
     * @throws MetricException if the a value has already been set on the metric.
     */
    public Builder setDoubleGaugeValue(double value) throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleGaugeValue(value);
      return this;
    }

    /**
     * Use a double summary value for the current metric. Will produce the entry
     * "gauge,min=[min],max=[max],sum=[sum],count=[count]" in the resulting metric line
     *
     * @param min the minimum value for the current metric.
     * @param max the maximum value for the current metric.
     * @param sum the sum of all values in the recorded timeframe
     * @param count the number of elements contributing to the value.
     * @return this.
     * @throws MetricException if the a value has already been set on the metric. Also thrown if min
     *     or max are greater than the sum or if the count is negative.
     */
    public Builder setDoubleSummaryValue(double min, double max, double sum, long count)
        throws MetricException {
      throwIfValueAlreadySet();
      this.value = new MetricValues.DoubleSummaryValue(min, max, sum, count);
      return this;
    }

    /**
     * (Optional) Set the {@link DimensionList} to be serialized. Either a single list of dimensions
     * or a merged list obtained from {@link DimensionList#merge} can be passed. When called
     * multiple times, this will overwrite previously added dimensions. If the builder was created
     * by the metric builder factory, default and OneAgent dimensions will be added to the passed
     * dimensions without having to manually merge them first.
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
     * <p>If the timestamp is from before the year 2000 or from after the year 3000 (e.g., when the
     * wrong unit was used when creating the {@link Instant}), the timestamp will be discarded and
     * no value will be set.
     *
     * @param timestamp an {@link Instant} object describing the time at which the {@link Metric}
     *     was created.
     * @return this
     */
    public Builder setTimestamp(Instant timestamp) {
      int year = timestamp.atZone(ZoneOffset.UTC).getYear();
      if (year < 2000 || year > 3000) {
        if (numberOfTimestampWarnings == 0) {
          logger.warning(
              "Order of magnitude of the timestamp seems off. "
                  + "The timestamp represents a time before the year 2000 or after the year 3000. "
                  + "Skipping setting timestamp, the current server time will be added upon ingestion. "
                  + "Only every 1000th of these messages will be printed.");
        }
        numberOfTimestampWarnings++;
        if (numberOfTimestampWarnings == 1000) {
          numberOfTimestampWarnings = 0;
        }

        // do not set the timestamp, metric will be exported without timestamp and the current
        // server timestamp is added upon ingestion.
        return this;
      }

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
     * @throws MetricException If no value is set or if the prefix/metric key combination evaluates
     *     to an invalid/empty metric key after normalization.
     */
    public String serialize() throws MetricException {
      String normalizedKeyString = makeNormalizedMetricKey();
      if (normalizedKeyString == null || normalizedKeyString.isEmpty()) {
        throw new MetricException("Normalized metric key is empty.");
      }

      if (this.value == null) {
        throw new MetricException("No value set for metric.");
      }

      // the two required arguments, metric key and value, are set and valid, so we start assembling
      // the metric line here.
      StringBuilder builder = new StringBuilder(normalizedKeyString);

      // combine default dimensions, dynamic dimensions and OneAgent dimensions into one list.
      DimensionList allDimensions =
          DimensionList.merge(this.defaultDimensions, this.dimensions, this.oneAgentDimensions);
      String dimensionsString = null;
      if (!allDimensions.isEmpty()) {
        dimensionsString = allDimensions.serialize();
      }

      // if any dimensions are present, serialize and append them to the metric string.
      if (dimensionsString != null && !dimensionsString.isEmpty()) {
        builder.append(",");
        builder.append(dimensionsString);
      }
      builder.append(" ");

      // add the serialized value to the metric string.
      builder.append(this.value.serialize());

      // if a timestamp is set, add it to the metric string.
      if (this.time != null) {
        builder.append(" ");
        builder.append(time.toEpochMilli());
      }

      return builder.toString();
    }

    private String makeNormalizedMetricKey() {
      if (this.prefix == null || this.prefix.isEmpty()) {
        return Normalize.metricKey(metricKey);
      }

      return Normalize.metricKey(String.format("%s.%s", prefix, metricKey));
    }
  }

  /** Created using {@link Metric.Builder} */
  private Metric() {}

  /**
   * Create a new {@link Builder Metric.Builder} object.
   *
   * @param metricKey The metric key. A prefix can be added right away or using the method on the
   *     builder.
   * @return A new {@link Builder} object with the metricKey property set.
   */
  public static Builder builder(String metricKey) {
    return new Builder(metricKey);
  }
}
