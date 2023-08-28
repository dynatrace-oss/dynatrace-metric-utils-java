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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;

class MetricLineBuilderImpl
    implements MetricLineBuilder.MetricKeyStep,
        MetricLineBuilder.TypeStep,
        MetricLineBuilder.GaugeStep,
        MetricLineBuilder.CounterStep,
        MetricLineBuilder.TimestampOrBuildStep,
        MetricLineBuilder.BuildStep {
  private static final Logger logger = Logger.getLogger(MetricLinePreConfiguration.class.getName());
  private static final AtomicInteger timestampWarningCounter = new AtomicInteger(0);
  private static final int TIMESTAMP_WARNING_THROTTLE_FACTOR = 1000;
  private static final int MINIMUM_CAPACITY = 72; // arbitrary, still better than 16
  private static final String PREFIX_STRING = "[%s] %s";

  private final MetricLinePreConfiguration preConfig;
  private final Map<String, String> dimensions = new HashMap<>();
  private String metricKey;
  private String type;
  private int descriptorLength;
  private int dimensionCount;

  // Used to hold the payload portion of the line (' gauge,5 <timestamp>')
  private StringBuilder payloadBuilder;

  private MetricLineBuilderImpl(MetricLinePreConfiguration preConfig) {
    this.preConfig = preConfig;

    this.dimensionCount =
        this.preConfig.getDefaultDimensions().size()
            + this.preConfig.getDynatraceMetadataDimensions().size();
    this.descriptorLength = this.preConfig.preConfigSerializedLength();
  }

  /**
   * Create a new {@link MetricLineBuilder.MetricKeyStep}-object that can be used to create a metric
   * line.
   *
   * @param preConfig The pre-configuration object containing shared data.
   * @return The created {@link MetricLineBuilder.MetricKeyStep} instance, with the given {@link
   *     MetricLinePreConfiguration}.
   */
  static MetricLineBuilder.MetricKeyStep builder(MetricLinePreConfiguration preConfig) {
    return new MetricLineBuilderImpl(preConfig);
  }

  @Override
  public MetricLineBuilder.TypeStep metricKey(String key) throws MetricException {
    if (StringValueValidator.isNullOrEmpty(key)) {
      throw new MetricException(ValidationMessages.METRIC_EMPTY_KEY_MESSAGE);
    }

    this.metricKey =
        StringValueValidator.isNullOrEmpty(this.preConfig.getPrefix())
            ? key
            : this.preConfig.getPrefix() + CodePoints.DOT_AS_STRING + key;

    // to avoid unnecessary object creation check if normalization is even required
    if (MetricKeyValidator.normalizationRequired(this.metricKey)) {
      NormalizationResult normalizationResult = Normalizer.normalizeMetricKey(this.metricKey);

      if (normalizationResult.messageType() == NormalizationResult.MessageType.WARNING) {
        logger.warning(() -> normalizationResult.getMessage());
      } else if (normalizationResult.messageType() == NormalizationResult.MessageType.ERROR) {
        throw new MetricException(
            String.format(ValidationMessages.METRIC_DROPPED_AFTER_NORMALIZATION_MESSAGE, key));
      }

      this.metricKey = normalizationResult.getResult();
    }

    if (this.descriptorLength + this.metricKey.length()
        > MetricLineConstants.Limits.MAX_LINE_LENGTH) {
      throw new MetricException(
          String.format(ValidationMessages.MAX_LINE_LENGTH_REACHED_WITH_METRIC_KEY_MESSAGE, key));
    }

    this.descriptorLength += this.metricKey.length();
    return this;
  }

  @Override
  public MetricLineBuilder.TypeStep dimension(String key, String value) throws MetricException {
    if (StringValueValidator.isNullOrEmpty(key)) {
      logger.warning(
          () ->
              String.format(
                  ValidationMessages.DIMENSION_DROPPED_KEY_EMPTY_WITH_METRIC_KEY_MESSAGE,
                  this.metricKey));
      return this;
    }

    String normalizedKey = key;

    // to avoid unnecessary object creation check if normalization is even required
    if (DimensionKeyValidator.normalizationRequired(key)) {
      NormalizationResult normalizationResult = Normalizer.normalizeDimensionKey(key);

      normalizedKey = normalizationResult.getResult();
      if (normalizationResult.messageType() != NormalizationResult.MessageType.NONE) {
        logger.warning(
            () -> String.format(PREFIX_STRING, this.metricKey, normalizationResult.getMessage()));
      }
    }

    // To avoid duplicate entries, which would be overwritten by values from
    // dynatraceMetadataDimensions during serialization, only add those entries (keys) from the
    // dimensions that don't exist in the dynatraceMetadataDimensions. Therefore, all
    // keys already in dynatraceMetadataDimensions are skipped, and no de-duplication needs to be
    // done on serialization.
    if (this.preConfig.getDynatraceMetadataDimensions().containsKey(normalizedKey)) {
      logger.info(
          () ->
              String.format(
                  ValidationMessages.DIMENSION_DROPPED_KEY_OVERWRITTEN_WITH_METRIC_KEY_MESSAGE,
                  this.metricKey,
                  value,
                  key));
      return this;
    }

    NormalizationResult normalizedValue =
        Normalizer.normalizeDimensionValue(
            value, MetricLineConstants.Limits.MAX_DIMENSION_VALUE_LENGTH);
    if (normalizedValue.messageType() != NormalizationResult.MessageType.NONE) {
      logger.warning(
          () -> String.format(PREFIX_STRING, this.metricKey, normalizedValue.getMessage()));
    }

    // only increase the dimensionCount, if this key-value-pair isn't already existing in the
    // defaultDimensions to preserve a valid dimensionsCount.
    tryAddDimension(
        normalizedKey,
        normalizedValue.getResult(),
        !this.preConfig.getDefaultDimensions().containsKey(normalizedKey));
    return this;
  }

  @Override
  public MetricLineBuilder.TypeStep dimensions(Map<String, String> dimensions)
      throws MetricException {
    if (dimensions == null || dimensions.isEmpty()) {
      return this;
    }

    if (dimensions.size() > MetricLineConstants.Limits.MAX_DIMENSIONS_COUNT) {
      throw new MetricException(
          String.format(
              ValidationMessages.TOO_MANY_DIMENSIONS_WITH_METRIC_KEY_MESSAGE, this.metricKey));
    }

    for (Map.Entry<String, String> entry : dimensions.entrySet()) {
      dimension(entry.getKey(), entry.getValue());
    }

    return this;
  }

  @Override
  public MetricLineBuilder.GaugeStep gauge() {
    this.type = MetricLineConstants.PayloadGauge.GAUGE;
    return this;
  }

  @Override
  public MetricLineBuilder.CounterStep count() {
    this.type = MetricLineConstants.PayloadCount.COUNT;
    return this;
  }

  @Override
  public MetricLineBuilder.TimestampOrBuildStep summary(
      double min, double max, double sum, long count) throws MetricException {
    BooleanResultMessage result = NumberValueValidator.isSummaryValid(min, max, sum, count);
    if (!result.isValid()) {
      throw new MetricException(String.format(PREFIX_STRING, this.metricKey, result.getMessage()));
    }

    this.payloadBuilder =
        new StringBuilder(MINIMUM_CAPACITY)
            .append(MetricLineConstants.PayloadGauge.MIN)
            .append(Normalizer.doubleToString(min))
            .append(MetricLineConstants.PayloadGauge.MAX)
            .append(Normalizer.doubleToString(max))
            .append(MetricLineConstants.PayloadGauge.SUM)
            .append(Normalizer.doubleToString(sum))
            .append(MetricLineConstants.PayloadGauge.COUNT)
            .append(count);

    return this;
  }

  @Override
  public MetricLineBuilder.TimestampOrBuildStep value(double value) throws MetricException {
    BooleanResultMessage result = NumberValueValidator.isValueValid(value);
    if (!result.isValid()) {
      throw new MetricException(String.format(PREFIX_STRING, this.metricKey, result.getMessage()));
    }

    this.payloadBuilder =
        new StringBuilder(MINIMUM_CAPACITY).append(Normalizer.doubleToString(value));
    return this;
  }

  @Override
  public MetricLineBuilder.TimestampOrBuildStep delta(double delta) throws MetricException {
    BooleanResultMessage result = NumberValueValidator.isValueValid(delta);
    if (!result.isValid()) {
      throw new MetricException(String.format(PREFIX_STRING, this.metricKey, result.getMessage()));
    }

    this.payloadBuilder =
        new StringBuilder(MINIMUM_CAPACITY)
            .append(MetricLineConstants.PayloadCount.DELTA)
            .append(Normalizer.doubleToString(delta));
    return this;
  }

  @Override
  public MetricLineBuilder.BuildStep timestamp(Instant timestamp) {
    if (timestamp == null) {
      logger.warning(
          () -> String.format(ValidationMessages.SKIP_INVALID_TIMESTAMP_MESSAGE, this.metricKey));
      return this;
    }

    int year = timestamp.atZone(ZoneOffset.UTC).getYear();
    if (year < 2000 || year > 3000) {
      if (timestampWarningCounter.getAndIncrement() == 0) {
        logger.warning(
            () ->
                String.format(
                    ValidationMessages.TIMESTAMP_OUT_OF_RANGE_MESSAGE,
                    this.metricKey,
                    timestamp,
                    TIMESTAMP_WARNING_THROTTLE_FACTOR));
      }
      timestampWarningCounter.compareAndSet(TIMESTAMP_WARNING_THROTTLE_FACTOR, 0);

      // do not set the timestamp, metric will be exported without timestamp and the current
      // server timestamp is added upon ingestion.
      return this;
    }

    this.payloadBuilder.appendCodePoint(CodePoints.BLANK).append(timestamp.toEpochMilli());
    return this;
  }

  @Override
  public MetricLineBuilder.MetadataStep metadata() {
    return new MetadataLineBuilderImpl();
  }

  @Override
  public String build() throws MetricException {
    StringBuilder serializedMetricLine =
        new StringBuilder(
            this.descriptorLength
                + Character.charCount(CodePoints.BLANK)
                + this.type.length()
                + Character.charCount(CodePoints.COMMA)
                + this.payloadBuilder.length());

    // serialize metric key
    serializedMetricLine.append(this.metricKey);

    // serialize dimensions
    // To avoid merging expenses of keys that occur in multiple dimension-maps, we already filtered
    // all double entries that are overwritten by higher-order importance. To exclude remaining
    // lower-order importance of defaultDimensions, we ignore all keys that are also existing in the
    // dimensions-map (defaultDimensions < dimensions < dynatraceMetadataDimensions).
    serializeDimensionMapAndAppend(
        serializedMetricLine, this.preConfig.getDefaultDimensions(), this.dimensions::containsKey);
    serializeDimensionMapAndAppend(serializedMetricLine, this.dimensions, (key) -> false);
    serializeDimensionMapAndAppend(
        serializedMetricLine, this.preConfig.getDynatraceMetadataDimensions(), (key) -> false);

    // serialize type and payload
    serializedMetricLine // prefix.metric.key,dim1=val1,...
        .appendCodePoint(CodePoints.BLANK) // ' '
        .append(this.type) // gauge
        .appendCodePoint(CodePoints.COMMA) // ,
        .append(this.payloadBuilder); // 10.5 timestamp

    if (serializedMetricLine.length() > MetricLineConstants.Limits.MAX_LINE_LENGTH) {
      throw new MetricException(
          String.format(
              ValidationMessages.MAX_LINE_LENGTH_REACHED_WITH_METRIC_KEY_MESSAGE, this.metricKey));
    }

    return serializedMetricLine.toString();
  }

  /**
   * Attempts to append the dimension to the metric line. It does so by checking if this additional
   * dimension, would overflow the limit of {@value MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT}
   * dimensions.
   *
   * @param normalizedKey The dimension key.
   * @param normalizedValue The dimension value.
   * @param shouldIncrement A flag that decides, if the dimensionCount should be increased
   * @throws MetricException if the dimension limit of {@value
   *     MetricLineConstants.Limits#MAX_DIMENSIONS_COUNT} would be overflowed after adding this
   *     dimension.
   */
  private void tryAddDimension(
      String normalizedKey, String normalizedValue, boolean shouldIncrement)
      throws MetricException {
    if (StringValueValidator.isNullOrEmpty(normalizedKey)) {
      logger.warning(
          () ->
              String.format(
                  ValidationMessages.DIMENSION_DROPPED_KEY_EMPTY_WITH_METRIC_KEY_MESSAGE,
                  this.metricKey));
      return;
    }

    if (this.dimensionCount + 1 > MetricLineConstants.Limits.MAX_DIMENSIONS_COUNT) {
      throw new MetricException(
          String.format(
              ValidationMessages.TOO_MANY_DIMENSIONS_WITH_METRIC_KEY_MESSAGE, this.metricKey));
    }

    // no check for max metric line length needed, because double entries in the different maps
    // cannot be considered and therefore this check at this point is irrelevant (in comparison to
    // {@link MetricLinePreConfiguration#tryAddDimensionTo})

    if (shouldIncrement) {
      this.dimensionCount++;
    }

    this.descriptorLength +=
        normalizedKey.length()
            + Character.charCount(CodePoints.DOT)
            + normalizedValue.length()
            + Character.charCount(CodePoints.COMMA);
    this.dimensions.put(normalizedKey, normalizedValue);
  }

  /**
   * Serializes given {@link Map dimensionsToSerialize}, if the given conditions allows it, and
   * appends it to provided {@link StringBuilder sb}.
   *
   * @param sb The StringBuilder where the serialized dimensions should be appended to.
   * @param dimensionsToSerialize The dimensions that should be serialized.
   * @param shouldBeIgnored A condition which determines for each dimension key, if it should be
   *     added.
   */
  private void serializeDimensionMapAndAppend(
      StringBuilder sb,
      Map<String, String> dimensionsToSerialize,
      Predicate<String> shouldBeIgnored) {
    for (Map.Entry<String, String> entry : dimensionsToSerialize.entrySet()) {
      if (StringValueValidator.isNullOrEmpty(entry.getValue())) {
        logger.warning(
            () ->
                String.format(
                    ValidationMessages.DIMENSION_NOT_SERIALIZED_OF_EMPTY_VALUE,
                    this.metricKey,
                    entry.getKey()));
        continue;
      }

      // If the provided condition is met, the key-value pair is not serialized, because the same
      // key has already been serialized or is going to be serialized after. Therefore, this
      // condition controls that this key isn't serialized multiple times.
      if (shouldBeIgnored.test(entry.getKey())) {
        continue;
      }

      sb.appendCodePoint(CodePoints.COMMA)
          .append(entry.getKey())
          .appendCodePoint(CodePoints.EQUALS)
          .append(entry.getValue());
    }
  }

  /**
   * A builder implementation for metadata lines, to separate {@code MetricLineBuilder-objects} from
   * {@code MetadataLineBuilder-objects}, to further allow working with different {@code objects} to
   * reduce interference. Creation is only possible through {@link
   * MetricLineBuilderImpl#metadata()}, which guarantees that necessary information is shared
   * between the metric line builder and the metadata line builder, as intended through the subclass
   * format. The builder performs validation and normalization, before serialization to ensure valid
   * metadata lines for ingestion into Dynatrace-API.
   */
  public class MetadataLineBuilderImpl implements MetricLineBuilder.MetadataStep {
    private String unit = null;
    private String description = null;
    private String displayName = null;

    private MetadataLineBuilderImpl() {}

    @Override
    public MetricLineBuilder.MetadataStep description(String description) {
      this.description = description;
      return this;
    }

    @Override
    public MetricLineBuilder.MetadataStep displayName(String name) {
      this.displayName = name;
      return this;
    }

    @Override
    public MetricLineBuilder.MetadataStep unit(String unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public String build() {
      StringBuilder payload = new StringBuilder(MINIMUM_CAPACITY);

      if (this.description != null) {
        if (StringValueValidator.isNullOrEmpty(this.description)
            || StringValueValidator.isEmptyQuoted(this.description)) {
          logger.warning(
              () ->
                  String.format(
                      MetadataLineConstants.ValidationMessages.DESCRIPTION_DROPPED_MESSAGE,
                      metricKey,
                      this.description));
        } else {
          NormalizationResult normalizationResult =
              Normalizer.normalizeDimensionValue(
                  this.description, MetadataLineConstants.Limits.MAX_DESCRIPTION_LENGTH);
          if (normalizationResult.messageType() != NormalizationResult.MessageType.NONE) {
            logger.warning(
                () -> String.format(PREFIX_STRING, metricKey, normalizationResult.getMessage()));
          }

          String normalizedDescription = normalizationResult.getResult();
          if (StringValueValidator.isNullOrEmpty(normalizedDescription)
              || StringValueValidator.isEmptyQuoted(normalizedDescription)) {
            logger.warning(
                () ->
                    String.format(
                        MetadataLineConstants.ValidationMessages.DESCRIPTION_DROPPED_MESSAGE,
                        metricKey,
                        this.description));
          } else {
            payload
                .append(MetadataLineConstants.Dimensions.DESCRIPTION_KEY)
                .appendCodePoint(CodePoints.EQUALS)
                .append(normalizedDescription);
          }
        }
      }

      if (this.unit != null) {
        if (!UnitValidator.isValidUnit(this.unit)) {
          logger.warning(
              () ->
                  String.format(
                      MetadataLineConstants.ValidationMessages.UNIT_DROPPED_MESSAGE,
                      metricKey,
                      this.unit));
        } else {
          if (payload.length() > 0) {
            payload.appendCodePoint(CodePoints.COMMA);
          }

          payload
              .append(MetadataLineConstants.Dimensions.UNIT_KEY)
              .appendCodePoint(CodePoints.EQUALS)
              .append(this.unit);
        }
      }

      if (this.displayName != null) {
        if (StringValueValidator.isNullOrEmpty(this.displayName)
            || StringValueValidator.isEmptyQuoted(this.displayName)) {
          logger.warning(
              () ->
                  String.format(
                      MetadataLineConstants.ValidationMessages.DISPLAY_NAME_DROPPED_MESSAGE,
                      metricKey,
                      this.displayName));
        } else {
          NormalizationResult normalizationResult =
              Normalizer.normalizeDimensionValue(
                  this.displayName, MetadataLineConstants.Limits.MAX_DISPLAY_NAME_LENGTH);
          if (normalizationResult.messageType() != NormalizationResult.MessageType.NONE) {
            logger.warning(
                () -> String.format(PREFIX_STRING, metricKey, normalizationResult.getMessage()));
          }

          String normalizedDisplayName = normalizationResult.getResult();
          if (StringValueValidator.isNullOrEmpty(normalizedDisplayName)
              || StringValueValidator.isEmptyQuoted(normalizedDisplayName)) {
            logger.warning(
                () ->
                    String.format(
                        MetadataLineConstants.ValidationMessages.DISPLAY_NAME_DROPPED_MESSAGE,
                        metricKey,
                        this.displayName));
          } else {
            if (payload.length() > 0) {
              payload.appendCodePoint(CodePoints.COMMA);
            }

            payload
                .append(MetadataLineConstants.Dimensions.DISPLAY_NAME_KEY)
                .appendCodePoint(CodePoints.EQUALS)
                .append(normalizedDisplayName);
          }
        }
      }

      // without any content, serialization does not make any sense
      if (payload.length() == 0) {
        logger.warning(
            () ->
                String.format(
                    MetadataLineConstants.ValidationMessages
                        .METADATA_SERIALIZATION_NOT_POSSIBLE_MESSAGE,
                    metricKey));
        return null;
      }

      StringBuilder sb =
          new StringBuilder(
              Character.charCount(CodePoints.NUMBER_SIGN)
                  + metricKey.length()
                  + Character.charCount(CodePoints.BLANK)
                  + type.length()
                  + Character.charCount(CodePoints.BLANK)
                  + payload.length());

      return sb.appendCodePoint(CodePoints.NUMBER_SIGN) // #
          .append(metricKey) // prefix.metric.key
          .appendCodePoint(CodePoints.BLANK) // ' '
          .append(type) // gauge
          .appendCodePoint(CodePoints.BLANK) // ' '
          .append(payload) // dt.meta.unit=Byte, ...
          .toString();
    }
  }
}
