/**
 * Copyright 2023 Dynatrace LLC
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
import java.util.Map;

/**
 * A builder interface that allows constructing metric and metadata lines. The builder performs
 * validation and normalization before serialization to ensure valid metric/metadata lines for
 * ingestion into Dynatrace API.
 */
public interface MetricLineBuilder {

  /** @return A {@link MetricKeyStep} with an empty pre-configuration object. */
  static MetricKeyStep create() {
    return MetricLineBuilderImpl.builder(MetricLinePreConfiguration.empty());
  }

  /**
   * @param preConfig The pre-configuration object containing shared data.
   * @return A {@link MetricKeyStep} with the given pre-configuration object.
   */
  static MetricKeyStep create(MetricLinePreConfiguration preConfig) {
    return MetricLineBuilderImpl.builder(preConfig);
  }

  interface MetricKeyStep {

    /**
     * Sets the metric key of the metric line. The key will be normalized.
     *
     * @param key The metric key of the metric line.
     * @return A {@link TypeStep}.
     * @throws MetricException if the key is invalid and therefore cannot be normalized.
     */
    TypeStep metricKey(String key) throws MetricException;
  }

  /** Interface to set the dimensions of the metric and to decide which type it is. */
  interface TypeStep {

    /**
     * Sets a dimension of the metric line. The key and value will be normalized. If a value is
     * already present for that key, it will be overwritten.
     *
     * @param key dimension key.
     * @param value dimension value.
     * @return A {@link TypeStep}.
     * @throws MetricException if the dimension limit of {@value
     *     MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT} would be exceeded after adding this
     *     dimension.
     */
    TypeStep dimension(String key, String value) throws MetricException;

    /**
     * Sets multiple dimensions (see {@link TypeStep#dimension}). Duplicate keys will be
     * overwritten.
     *
     * @param dimensions The dimensions to be added.
     * @return A {@link TypeStep}.
     * @throws MetricException if the dimension limit of {@value
     *     MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT} would be exceeded after adding this
     *     dimension.
     */
    TypeStep dimensions(Map<String, String> dimensions) throws MetricException;

    /**
     * Sets the metric line type to gauge (summary or single value).
     *
     * @return A {@link GaugeStep} that can be used to set the gauge value.
     */
    GaugeStep gauge();

    /**
     * Sets the metric line type to counter.
     *
     * @return A {@link CounterStep} that can be used to set the delta value.
     */
    CounterStep count();
  }

  interface GaugeStep {
    /**
     * Sets the summary value of a gauge data point. It summarizes multiple values e.g., values:
     * [1,1,1,1,2,3] - min: 1, max: 3, sum: 9, count: 6.
     *
     * @param min The min value.
     * @param max The max value.
     * @param sum The sum of values.
     * @param count The number of observations.
     * @return A {@link TimestampOrBuildStep} that can be used to set the timestamp or build the
     *     metric line.
     * @throws MetricException if any of the stat counter values constraints are violated.
     */
    TimestampOrBuildStep summary(double min, double max, double sum, long count)
        throws MetricException;

    /**
     * Sets the gauge value.
     *
     * @param value The gauge value.
     * @return A {@link TimestampOrBuildStep} that can be used to set the timestamp or build the
     *     metric line.
     * @throws MetricException if the gauge value is NaN or +/- infinity.
     */
    TimestampOrBuildStep value(double value) throws MetricException;

    /**
     * Creates a {@code MetadataLineBuilder} with the metric key and type pre-filled.
     *
     * @return A {@link MetadataStep} object to further configure and serialize a metadata line.
     */
    MetadataStep metadata();
  }

  interface CounterStep {
    /**
     * Sets the counter delta value. The delta value is the change in the counter since the last
     * export.
     *
     * @param delta The counter's delta value.
     * @return A {@link TimestampOrBuildStep} that can be used to set the timestamp or build the
     *     metric line.
     * @throws MetricException if the counter value is NaN or +/- infinity.
     */
    TimestampOrBuildStep delta(double delta) throws MetricException;

    /**
     * Creates a {@code MetadataLineBuilder} with the metric key and type pre-filled.
     *
     * @return A {@link MetadataStep} object to further configure and serialize a metadata line.
     */
    MetadataStep metadata();
  }

  interface TimestampOrBuildStep extends BuildStep {

    /**
     * Sets the (optional) timestamp of the data point.
     *
     * @param timestamp The timestamp value.
     * @return A {@link BuildStep} that can be used to serialize the metric line.
     */
    BuildStep timestamp(Instant timestamp);
  }

  interface BuildStep {
    /**
     * Serialize the metric line with all normalizations applied.
     *
     * @return The metric line as a {@link String}.
     * @throws MetricException if the max line length limit of {@value
     *     MetricLineConstants.Limits#MAX_LINE_LENGTH} would be reached.
     */
    String build() throws MetricException;
  }

  interface MetadataStep {

    /**
     * Sets the {@code dt.meta.description} dimension on the metadata line.
     *
     * @param description A short description of the metric.
     * @return A {@link MetadataStep} that can be used to build the metadata line or set other
     *     metadata settings.
     */
    MetadataStep description(String description);

    /**
     * Sets the {@code dt.meta.unit} dimension on the metadata line.
     *
     * @param unit The unit of the metric.
     * @return A {@link MetadataStep} that can be used to build the metadata line or set other
     *     metadata settings.
     */
    MetadataStep unit(String unit);

    /**
     * Sets the {@code dt.meta.displayName} dimension on the metadata line.
     *
     * @param name The display name of the metric.
     * @return A {@link MetadataStep} that can be used to build the metadata line or set other
     *     metadata settings.
     */
    MetadataStep displayName(String name);

    /**
     * Serializes the metadata line after normalization of provided properties.
     *
     * @return The metadata line as a {@link String} if properties are set, and {@code null}
     *     otherwise.
     */
    String build();
  }
}
