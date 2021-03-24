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

  public static MetricBuilderFactoryBuilder builder() {
    return new MetricBuilderFactoryBuilder();
  }

  public Metric.Builder newMetricBuilder(String metricName) {
    return Metric.builder(metricName)
        .setDefaultDimensions(defaultDimensions)
        .setOneAgentDimensions(oneAgentDimensions)
        .setPrefix(prefix);
  }

  public static class MetricBuilderFactoryBuilder {
    private DimensionList defaultDimensions;
    private boolean enrichWithOneAgentData;
    private String prefix;

    private MetricBuilderFactoryBuilder() {}

    public MetricBuilderFactoryBuilder withDefaultDimensions(DimensionList defaultDimensions) {
      this.defaultDimensions = defaultDimensions;
      return this;
    }

    public MetricBuilderFactoryBuilder withOneAgentMetadata() {
      this.enrichWithOneAgentData = true;
      return this;
    }

    public MetricBuilderFactoryBuilder withPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public MetricBuilderFactory build() {
      DimensionList localDefaultDimensions = null;
      DimensionList localOneAgentDimensions = null;
      String localPrefix = "";

      if (this.enrichWithOneAgentData) {
        localOneAgentDimensions =
            DimensionList.create(
                OneAgentMetadataEnricher.getDimensionsFromOneAgentMetadata()
                    .toArray(new Dimension[0]));
      }

      if (this.defaultDimensions != null) {
        localDefaultDimensions = this.defaultDimensions;
      }

      if (this.prefix != null) {
        localPrefix = this.prefix;
      }

      return new MetricBuilderFactory(localDefaultDimensions, localOneAgentDimensions, localPrefix);
    }
  }
}
