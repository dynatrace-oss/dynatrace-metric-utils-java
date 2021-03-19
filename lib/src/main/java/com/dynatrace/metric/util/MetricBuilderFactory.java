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

// todo this is to be thought through again...
public class MetricBuilderFactory {
  private final DimensionList oneAgentDimensions;
  private final DimensionList defaultDimensions;

  private MetricBuilderFactory(DimensionList oneAgentDimensions, DimensionList defaultDimensions) {
    this.oneAgentDimensions = oneAgentDimensions;
    this.defaultDimensions = defaultDimensions;
  }

  private static MetricBuilderFactory doCreate(
      DimensionList defaultDimensions, DimensionList oneAgentDimensions) {
    DimensionList defaultList;
    DimensionList oneAgentList;
    if (defaultDimensions == null || defaultDimensions.getDimensions().isEmpty()) {
      defaultList = DimensionList.create();
    } else {
      // merge with one list removes duplicates.
      defaultList = DimensionList.merge(defaultDimensions);
    }

    if (oneAgentDimensions == null || oneAgentDimensions.getDimensions().isEmpty()) {
      oneAgentList = DimensionList.create();
    } else {
      oneAgentList = DimensionList.merge(oneAgentDimensions);
    }

    return new MetricBuilderFactory(defaultList, oneAgentList);
  }

  public static class MetricBuilderFactoryBuilder {
    private DimensionList defaultDimensions;
    private boolean enrichWithOneAgentData;

    private MetricBuilderFactoryBuilder() {}

    public MetricBuilderFactoryBuilder withDefaultDimensions(DimensionList defaultDimensions) {
      this.defaultDimensions = defaultDimensions;
      return this;
    }

    public MetricBuilderFactoryBuilder setEnrichWithOneAgentData(boolean enrich) {
      this.enrichWithOneAgentData = enrich;
      return this;
    }

    public MetricBuilderFactory build() {
      if (this.enrichWithOneAgentData) {
        OneAgentMetadataEnricher enricher = new OneAgentMetadataEnricher();
        DimensionList oneAgentDimensions =
            DimensionList.create(
                enricher.getDimensionsFromOneAgentMetadata().toArray(new Dimension[0]));

        // if the default dimensions are not set, do create will catch that.
        return doCreate(this.defaultDimensions, oneAgentDimensions);
      }
      return doCreate(this.defaultDimensions, null);
    }
  }
}
