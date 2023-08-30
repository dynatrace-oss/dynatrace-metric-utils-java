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

/** Constants related to metric line creation, serialization and normalization. */
final class MetricLineConstants {

  private MetricLineConstants() {}

  /** Constants for metric length limits. */
  static final class Limits {

    private Limits() {}

    // Exceeding these causes the data point to be dropped
    static final int MAX_LINE_LENGTH = 50_000;
    static final int MAX_DIMENSIONS_COUNT = 50;

    // Exceeding these cause the values to be truncated.
    static final int MAX_METRIC_KEY_LENGTH = 250;
    static final int MAX_DIMENSION_KEY_LENGTH = 100;
    static final int MAX_DIMENSION_VALUE_LENGTH = 250;
  }

  /** Constants for Gauge payload creation. */
  static final class PayloadGauge {

    private PayloadGauge() {}

    static final String GAUGE = "gauge";
    static final String MIN = "min=";
    static final String MAX = ",max=";
    static final String SUM = ",sum=";
    static final String COUNT = ",count=";
  }

  /** Constants for Counter payload creation. */
  static final class PayloadCount {

    private PayloadCount() {}

    public static final String COUNT = "count";
    public static final String DELTA = "delta=";
  }

  /** Validation messages concerning errors and warnings of the metric line builder. */
  static final class ValidationMessages {

    private ValidationMessages() {}

    // errors
    static final String METRIC_EMPTY_KEY_MESSAGE = "Metric key is empty";
    static final String METRIC_DROPPED_AFTER_NORMALIZATION_MESSAGE =
        "Metric key invalid after normalization. Pre-normalization key: '%s'";

    static final String GAUGE_COUNT_NEGATIVE_MESSAGE =
        "count < 0: min: %f, max %f, sum: %f, count: %d";
    static final String GAUGE_COUNT_ZERO_MESSAGE =
        "count == 0 => min, max, sum != 0: min: %f, max %f, sum: %f, count: %d";
    static final String GAUGE_INFINITE_MESSAGE =
        "infinite values: min: %f, max %f, sum: %f, count: %d";
    static final String GAUGE_NAN_MESSAGE = "NaN values: min: %f, max %f, sum: %f, count: %d";
    static final String GAUGE_MIN_GREATER_MAX_MESSAGE =
        "max < min: min: %f, max: %f, sum: %f, count: %d";

    static final String VALUE_NAN_MESSAGE = "Metric value was NaN";
    static final String VALUE_INFINITE_MESSAGE = "Metric value was infinite (%f)";

    static final String MAX_LINE_LENGTH_REACHED_MESSAGE =
        "Serialized line exceeds limit of "
            + Limits.MAX_LINE_LENGTH
            + " characters accepted by the ingest API'";
    static final String MAX_LINE_LENGTH_REACHED_WITH_METRIC_KEY_MESSAGE =
        "[%s] " + MAX_LINE_LENGTH_REACHED_MESSAGE;

    static final String TOO_MANY_DIMENSIONS_MESSAGE =
        "Too many dimensions were tried to be set, max limit of "
            + Limits.MAX_DIMENSIONS_COUNT
            + " surpassed";
    static final String TOO_MANY_DIMENSIONS_WITH_METRIC_KEY_MESSAGE =
        "[%s] " + TOO_MANY_DIMENSIONS_MESSAGE;

    // warnings
    static final String METRIC_KEY_NORMALIZED_MESSAGE = "Metric key normalized from: '%s' to: '%s'";

    static final String DIMENSION_KEY_NORMALIZED_MESSAGE =
        "Dimension key normalized from: '%s' to: '%s'";
    static final String DIMENSION_VALUE_NORMALIZED_MESSAGE =
        "Dimension value normalized from: '%s' to: '%s'";
    static final String DIMENSION_DROPPED_KEY_EMPTY_MESSAGE =
        "Dimension with empty dimension key has been dropped";
    static final String DIMENSION_DROPPED_KEY_EMPTY_WITH_METRIC_KEY_MESSAGE =
        "[%s] " + DIMENSION_DROPPED_KEY_EMPTY_MESSAGE;
    static final String DIMENSION_NOT_SERIALIZED_OF_EMPTY_VALUE =
        "[%s] Dimension value for dimension key '%s' is null or empty";
    static final String SKIP_EMPTY_DYNATRACE_METADATA_DIMENSIONS =
        "Received empty Dynatrace metadata dimensions. Continuing without Dynatrace metadata.";
    static final String SKIP_EMPTY_DEFAULT_DIMENSIONS =
        "Received empty default dimensions. Continuing without default dimensions.";
    static final String DIMENSION_DROPPED_KEY_OVERWRITTEN_MESSAGE =
        "Dimension value '%s' for key '%s' skipped: Using value from pre-configuration instead.";
    static final String DIMENSION_DROPPED_KEY_OVERWRITTEN_WITH_METRIC_KEY_MESSAGE =
        "[%s] " + DIMENSION_DROPPED_KEY_OVERWRITTEN_MESSAGE;

    static final String SKIP_INVALID_TIMESTAMP_MESSAGE =
        "[%s] Skip setting timestamp, because it is null";
    static final String TIMESTAMP_OUT_OF_RANGE_MESSAGE =
        "[%s] Order of magnitude of the timestamp seems off (%s). "
            + "The timestamp represents a time before the year 2000 or after the year 3000. "
            + "Skipping setting timestamp, the current server time will be added upon ingestion. "
            + "Only one out of every %d of these messages will be printed.";
  }
}
