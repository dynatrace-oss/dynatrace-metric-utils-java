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

import com.dynatrace.metric.util.MetricLineConstants.ValidationMessages;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * A pre-configuration object that holds prefix, default dimensions and Dynatrace metadata
 * dimensions. It can be passed to a {@link MetricLineBuilder} object via {@link
 * MetricLineBuilderImpl#builder(MetricLinePreConfiguration)} to create multiple {@link
 * MetricLineBuilder} objects with the same pre-configuration.
 */
public class MetricLinePreConfiguration {
  private static final Logger logger = Logger.getLogger(MetricLinePreConfiguration.class.getName());
  private static final NormalizationWarnThenDebugLogger normalizationLogger =
      new NormalizationWarnThenDebugLogger(logger);
  private static final String CLASS_NAME_FOR_LOGGING =
      String.format("{%s}", MetricLinePreConfiguration.class.getSimpleName());
  private static final MetricLinePreConfiguration EMPTY_PRE_CONFIG =
      new MetricLinePreConfiguration(null, Collections.emptyMap(), Collections.emptyMap(), 0);

  private final Map<String, String> dynatraceMetadataDimensions;
  private final Map<String, String> defaultDimensions;
  private final String prefix;
  private final int serializationLength;

  private MetricLinePreConfiguration(
      String prefix,
      Map<String, String> defaultDimensions,
      Map<String, String> dynatraceMetadataDimensions,
      int serializationLength) {
    this.prefix = prefix;
    this.defaultDimensions = defaultDimensions;
    this.dynatraceMetadataDimensions = dynatraceMetadataDimensions;
    this.serializationLength = serializationLength;
  }

  public Map<String, String> getDynatraceMetadataDimensions() {
    return dynatraceMetadataDimensions;
  }

  public Map<String, String> getDefaultDimensions() {
    return defaultDimensions;
  }

  public String getPrefix() {
    return prefix;
  }

  /** @return The length of the String, if you would serialize all attributes */
  int preConfigSerializedLength() {
    return serializationLength;
  }

  /** @return an empty {@link MetricLinePreConfiguration}-object. */
  static MetricLinePreConfiguration empty() {
    return EMPTY_PRE_CONFIG;
  }

  /**
   * Create a new {@link Builder} that can be used to set up a {@link MetricLinePreConfiguration}.
   *
   * @return The created {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for {@link MetricLinePreConfiguration} objects. */
  public static class Builder {

    private Map<String, String> defaultDimensions = null;
    private boolean withDynatraceMetadataDimensions = false;
    private String prefix;
    private int dimensionCount = 0;
    private int serializationLength = 0;

    private Builder() {}

    /**
     * Set a common prefix that will be prepended to all metric keys that are using the {@link
     * MetricLinePreConfiguration} object.
     *
     * @param prefix The prefix to be added to metric keys.
     * @return this
     */
    public Builder prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Set default dimensions which will be added to all metric lines created using this {@link
     * MetricLinePreConfiguration} object.
     *
     * @param defaultDimensions A {@link Map} containing default dimensions
     * @return this
     * @throws MetricException if the provided {@code defaultDimensions} size exceeds the limit of
     *     {@value MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT}.
     */
    public Builder defaultDimensions(Map<String, String> defaultDimensions) throws MetricException {
      if (defaultDimensions.size() > MetricLineConstants.Limits.MAX_DIMENSIONS_COUNT) {
        throw new MetricException(ValidationMessages.TOO_MANY_DIMENSIONS_MESSAGE);
      }

      this.defaultDimensions = defaultDimensions;
      return this;
    }

    /**
     * If this method is called, Dynatrace metadata will automatically be pulled in and can be added
     * to {@link MetricLineBuilder} objects via the {@link MetricLinePreConfiguration} object. If
     * this method is not called, no Dynatrace metadata will be added.
     *
     * @return this
     */
    public Builder dynatraceMetadataDimensions() {
      this.withDynatraceMetadataDimensions = true;
      return this;
    }

    /**
     * Build the {@link MetricLinePreConfiguration} using the data provided by this {@link Builder}.
     * Necessary normalization is done.
     *
     * @return A {@link MetricLinePreConfiguration} that can be used by {@link MetricLineBuilder}
     *     objects.
     * @throws MetricException see {@link Builder#dimension(String, String, Map, Predicate)}
     */
    public MetricLinePreConfiguration build() throws MetricException {
      Map<String, String> normalizedDefaultDimensions = new HashMap<>();
      Map<String, String> normalizedDynatraceMetadataDimensions = new HashMap<>();

      this.dimensionCount = 0;
      this.serializationLength = 0;

      // First, add all dynatraceMetadataDimensions.
      if (this.withDynatraceMetadataDimensions) {
        Map<String, String> dynatraceMetadataDimensions =
            DynatraceMetadataEnricher.getDynatraceMetadata();

        if (dynatraceMetadataDimensions.isEmpty()) {
          logger.warning(() -> ValidationMessages.SKIP_EMPTY_DYNATRACE_METADATA_DIMENSIONS);
        } else {
          for (Map.Entry<String, String> entry : dynatraceMetadataDimensions.entrySet()) {
            dimension(
                entry.getKey(),
                entry.getValue(),
                normalizedDynatraceMetadataDimensions,
                (key) -> false);
          }
        }
      }

      // To avoid duplicate entries, which would be overwritten by values from
      // dynatraceMetadataDimensions during serialization, only add those entries (keys) from the
      // defaultDimensions that don't exist in the dynatraceMetadataDimensions. Therefore, all
      // keys already in dynatraceMetadataDimensions are skipped, and no de-duplication needs to be
      // done on serialization.
      if (this.defaultDimensions != null) {
        if (this.defaultDimensions.isEmpty()) {
          logger.warning(() -> ValidationMessages.SKIP_EMPTY_DEFAULT_DIMENSIONS);
        }

        for (Map.Entry<String, String> entry : this.defaultDimensions.entrySet()) {
          // key needs to be normalized before checking if it already exists, which is why the
          // `containsKey` method needs to be passed in here.
          dimension(
              entry.getKey(),
              entry.getValue(),
              normalizedDefaultDimensions,
              normalizedDynatraceMetadataDimensions::containsKey);
        }
      }

      return new MetricLinePreConfiguration(
          this.prefix,
          normalizedDefaultDimensions,
          normalizedDynatraceMetadataDimensions,
          this.serializationLength);
    }

    /**
     * Attempts to append a new dimension to the specified {@link Map targetDimensions}. This is
     * done by checking if adding the key does not violate the provided condition, would exceed the
     * limit of {@value MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT} dimensions. and if it will
     * cause the metric line to exceed the {@value MetricLineConstants.Limits#MAX_LINE_LENGTH}
     * length limit. The key and value are normalized.
     *
     * @param key The dimension key.
     * @param value The dimension value.
     * @param targetDimensions The Map that the dimension should be added to.
     * @param shouldBeIgnored A condition that determines whether a dimension should be ignored,
     *     based on the dimension key. added.
     * @throws MetricException see {@link Builder#tryAddDimensionTo(String, String, Map)}
     */
    private void dimension(
        String key,
        String value,
        Map<String, String> targetDimensions,
        Predicate<String> shouldBeIgnored)
        throws MetricException {
      if (StringValueValidator.isNullOrEmpty(key)) {
        logger.warning(() -> ValidationMessages.DIMENSION_DROPPED_KEY_EMPTY_MESSAGE);
        return;
      }

      String normalizedKey = key;
      if (DimensionKeyValidator.normalizationRequired(key)) {
        NormalizationResult normalizeKeyResult = Normalizer.normalizeDimensionKey(key);

        normalizedKey = normalizeKeyResult.getResult();
        if (normalizeKeyResult.messageType() != NormalizationResult.MessageType.NONE) {
          normalizationLogger.logDimensionKeyMessage(CLASS_NAME_FOR_LOGGING, normalizeKeyResult);
        }
      }

      // if the provided condition is met, the key-value would be overwritten during serialization,
      // therefore, it can be already ignored at this time.
      if (shouldBeIgnored.test(normalizedKey)) {
        logger.info(
            () ->
                String.format(
                    ValidationMessages.DIMENSION_DROPPED_KEY_OVERWRITTEN_MESSAGE, value, key));
        return;
      }

      NormalizationResult normalizeValueResult =
          Normalizer.normalizeDimensionValue(
              value, MetricLineConstants.Limits.MAX_DIMENSION_VALUE_LENGTH);
      if (normalizeValueResult.messageType() != NormalizationResult.MessageType.NONE) {
        normalizationLogger.logDimensionValueMessage(
            CLASS_NAME_FOR_LOGGING, normalizedKey, normalizeValueResult);
      }

      tryAddDimensionTo(normalizedKey, normalizeValueResult.getResult(), targetDimensions);
    }

    /**
     * Attempts to append new dimension to the specified dimensions. It does so by checking if this
     * additional dimension, would exceed the limit of {@value
     * MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT} dimensions. An exception will be thrown if
     * adding the dimension will cause the metric line to exceed the {@value
     * MetricLineConstants.Limits#MAX_LINE_LENGTH} length limit.
     *
     * @param normalizedKey The dimension key.
     * @param normalizedValue The dimension value.
     * @param targetDimensions The Map that the key-value-pair should be added to
     * @throws MetricException if appending the dimension will cause the metric line to exceed the
     *     {@value MetricLineConstants.Limits#MAX_LINE_LENGTH} length limit, or it will exceed the
     *     limit of {@value MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT} dimensions.
     */
    private void tryAddDimensionTo(
        String normalizedKey, String normalizedValue, Map<String, String> targetDimensions)
        throws MetricException {
      if (StringValueValidator.isNullOrEmpty(normalizedKey)) {
        logger.warning(() -> ValidationMessages.DIMENSION_DROPPED_KEY_EMPTY_MESSAGE);
        return;
      }

      if (this.dimensionCount + 1 > MetricLineConstants.Limits.MAX_DIMENSIONS_COUNT) {
        throw new MetricException(ValidationMessages.TOO_MANY_DIMENSIONS_MESSAGE);
      }

      this.serializationLength +=
          normalizedKey.length()
              + Character.charCount(CodePoints.EQUALS)
              + normalizedValue.length()
              + Character.charCount(CodePoints.COMMA);

      if (this.serializationLength > MetricLineConstants.Limits.MAX_LINE_LENGTH) {
        // The addition of the dimension will cause the line to exceed the 50000 limit
        // mark as error so the datapoint is dropped
        throw new MetricException(ValidationMessages.MAX_LINE_LENGTH_REACHED_MESSAGE);
      }

      this.dimensionCount++;
      targetDimensions.put(normalizedKey, normalizedValue);
    }
  }
}
