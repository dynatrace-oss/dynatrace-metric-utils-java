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
package com.dynatrace.metric.util.example;

import com.dynatrace.metric.util.*;

public class App {
  public static void main(String[] args) {
    DimensionList defaultDims =
        DimensionList.create(
            Dimension.create("default1", "value1"), Dimension.create("default2", "value2"));

    DimensionList dimensions =
        DimensionList.create(
            Dimension.create("dim1", "value1"), Dimension.create("dim2", "value2"));

    DimensionList differentDimensions =
        DimensionList.create(Dimension.create("differentDim", "differentValue"));
    // =============================================================================================
    // Version 1: using the MetricBuilderFactory
    // =============================================================================================
    // setup metric builder factory with items that can be shared between multiple metrics
    MetricBuilderFactory metricBuilderFactory =
        MetricBuilderFactory.builder()
            .withDefaultDimensions(defaultDims)
            .withOneAgentMetadata()
            .withPrefix("prefix")
            .build();

    try {
      // the following code will create this metric line:
      // prefix.metric1,dim2=value2,default1=value1,dim1=value1,default2=value2 count,123 1616416882
      String metricLine1 =
          metricBuilderFactory
              .newMetricBuilder("metric1")
              .setDimensions(dimensions)
              .setLongCounterValue(123)
              .setCurrentTime()
              .serialize();

      String metricLine2 =
          metricBuilderFactory
              .newMetricBuilder("metric2")
              .setDimensions(differentDimensions)
              .setLongCounterValue(321)
              .setCurrentTime()
              .serialize();

      System.out.println(metricLine1);
      System.out.println(metricLine2);

    } catch (MetricException me) {
      System.out.println(me.toString());
    }

    // =============================================================================================
    // Version 2: using the Metrics.Builder directly
    // =============================================================================================
    // this approach leaves reading and merging the dimensions to the user.
    DimensionList oneAgentDimensions = DimensionList.fromOneAgentMetadata();

    try {
      String metricLine1 =
          Metric.builder("metric1")
              .setPrefix("prefix")
              .setDimensions(DimensionList.merge(defaultDims, dimensions, oneAgentDimensions))
              .setLongCounterValue(123)
              .setCurrentTime()
              .serialize();

      String metricLine2 =
          Metric.builder("metric2")
              .setPrefix("prefix")
              .setDimensions(
                  DimensionList.merge(defaultDims, differentDimensions, oneAgentDimensions))
              .setLongCounterValue(321)
              .setCurrentTime()
              .serialize();

      System.out.println(metricLine1);
      System.out.println(metricLine2);

    } catch (MetricException me) {
      System.out.println(me.toString());
    }
  }
}
