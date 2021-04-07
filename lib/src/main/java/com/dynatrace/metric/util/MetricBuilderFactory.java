/**
 * * Copyright 2021 Dynatrace LLC
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

/** A factory that creates {@link Metric.Builder} objects with presets. */
public class MetricBuilderFactory {
  private final DimensionList oneAgentDimensions;
  private final DimensionList defaultDimensions;
  private final String prefix;

  private MetricBuilderFactory(
      DimensionList defaultDimensions, DimensionList oneAgentDimensions, String prefix) {
    this.oneAgentDimensions = oneAgentDimensions;
    this.defaultDimensions = defaultDimensions;
    this.prefix = prefix;
  }

  /**
   * Create a new {@link MetricBuilderFactoryBuilder} that can be used to set up a {@link
   * MetricBuilderFactory}.
   *
   * @return The created builder instance.
   */
  public static MetricBuilderFactoryBuilder builder() {
    return new MetricBuilderFactoryBuilder();
  }

  /**
   * Get a new {@link Metric.Builder} object that can be used to create metric lines. Prefix,
   * default dimensions and OneAgent dimensions set on the {@link MetricBuilderFactory} are already
   * set on the {@link Metric.Builder} object.
   *
   * @param metricKey the metric key (not including the prefix) for the new metric.
   * @return An instance of class {@link Metric.Builder} with default and OneAgent dimensions and
   *     the prefix set if set in the factory.
   */
  public Metric.Builder newMetricBuilder(String metricKey) {
    return Metric.builder(metricKey)
        .setDefaultDimensions(defaultDimensions)
        .setOneAgentDimensions(oneAgentDimensions)
        .setPrefix(prefix);
  }

  /** Builder class for {@link MetricBuilderFactory} objects. */
  public static class MetricBuilderFactoryBuilder {
    private DimensionList defaultDimensions;
    private boolean enrichWithOneAgentData;
    private String prefix;

    private MetricBuilderFactoryBuilder() {}

    /**
     * Set default dimensions on the {@link MetricBuilderFactory}. All {@link Metric.Builder}
     * objects created by this method already have those default dimensions set.
     *
     * @param defaultDimensions A {@link DimensionList} containing default dimensions
     * @return this
     */
    public MetricBuilderFactoryBuilder withDefaultDimensions(DimensionList defaultDimensions) {
      this.defaultDimensions = defaultDimensions;
      return this;
    }

    /**
     * If this method is called upon building the {@link MetricBuilderFactory} object, OneAgent
     * metadata will automatically be pulled in and added to all {@link Metric.Builder} objects
     * created by the factory. If this method is not called, the setting will default to false.
     *
     * @return this
     */
    public MetricBuilderFactoryBuilder withOneAgentMetadata() {
      this.enrichWithOneAgentData = true;
      return this;
    }

    /**
     * Set a common prefix that will be prepended to all metric keys that are created using {@link
     * Metric.Builder} objects created by this {@link MetricBuilderFactory}.
     *
     * @param prefix The prefix to be added to metric keys.
     * @return this
     */
    public MetricBuilderFactoryBuilder withPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Build the {@link MetricBuilderFactory} using the presets set using the "with" methods on the
     * {@link MetricBuilderFactoryBuilder} object.
     *
     * @return A {@link MetricBuilderFactory} that can be used to create {@link Metric.Builder}
     *     objects.
     */
    public MetricBuilderFactory build() {
      DimensionList localOneAgentDimensions = null;

      if (this.enrichWithOneAgentData) {
        localOneAgentDimensions = DimensionList.fromOneAgentMetadata();
      }

      return new MetricBuilderFactory(this.defaultDimensions, localOneAgentDimensions, this.prefix);
    }
  }
}
