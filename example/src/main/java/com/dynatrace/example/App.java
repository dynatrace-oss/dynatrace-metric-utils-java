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
package com.dynatrace.example;

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
      System.out.println(
          metricBuilderFactory
              .newMetricBuilder("metric1")
              .setDimensions(dimensions)
              .setIntCounterValue(123)
              .setCurrentTime()
              .buildAndSerialize());
      System.out.println(
          metricBuilderFactory
              .newMetricBuilder("metric2")
              .setDimensions(differentDimensions)
              .setIntCounterValue(321)
              .setCurrentTime()
              .buildAndSerialize());

      // prefix.metric1,dim2=value2,default1=value1,dim1=value1,default2=value2 count,123 1616416882
      // prefix.metric2,default1=value1,default2=value2,differentdim=differentValue count,321
      // 1616416882

    } catch (MetricException me) {
      System.out.println(me.getMessage());
    }

    // =============================================================================================
    // Version 2: using the Metrics.Builder directly
    // =============================================================================================
    // this approach leaves reading and merging the dimensions to the user.
    DimensionList oneAgentDimensions = DimensionList.fromOneAgentMetadata();

    try {
      System.out.println(
          Metric.builder("metric1")
              .setPrefix("prefix")
              .setDimensions(DimensionList.merge(defaultDims, dimensions, oneAgentDimensions))
              .setIntCounterValue(123)
              .setCurrentTime()
              .buildAndSerialize());

      System.out.println(
          Metric.builder("metric2")
              .setPrefix("prefix")
              .setDimensions(
                  DimensionList.merge(defaultDims, differentDimensions, oneAgentDimensions))
              .setIntCounterValue(321)
              .setCurrentTime()
              .buildAndSerialize());

      // output
      // prefix.metric1,dim2=value2,default1=value1,dim1=value1,default2=value2 count,123 1616416882
      // prefix.metric2,default1=value1,default2=value2,differentdim=differentValue count,321
      // 1616416882
    } catch (MetricException me) {
      System.out.println(me.getMessage());
    }
  }
}
