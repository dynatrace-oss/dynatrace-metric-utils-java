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

    DimensionList labels =
        DimensionList.create(
            Dimension.create("label1", "value1"), Dimension.create("label2", "value2"));

    DimensionList differentLabels =
        DimensionList.create(Dimension.create("differentDim", "differentValue"));
    // =============================================================================================
    // Version 1: using the MetricBuilderFactory
    // =============================================================================================
    // setup metric builder factory with items that can be shared between multiple metrics
    MetricBuilderFactory metricBuilderFactory =
        MetricBuilderFactory.builder()
            .withDefaultDimensions(defaultDims)
            .withOneAgentMetadata()
            .withLabels(labels)
            .withPrefix("prefix")
            .build();

    try {
      System.out.println(
          metricBuilderFactory
              .newMetricBuilder("metric1")
              .setIntCounterValue(123)
              .setCurrentTime()
              .build()
              .serialize());
      System.out.println(
          metricBuilderFactory
              .newMetricBuilder("metric2")
              .setDimensions(differentLabels)
              .setIntCounterValue(321)
              .setCurrentTime()
              .build()
              .serialize());

      // labels are overwritten when setting new dimensions, but the default labels are still
      // printed (second line). The same is true for OneAgent data, which is not shown here since
      // this was run on a pc without OneAgent installed.
      //      prefix.metric1,default1=value1,default2=value2,label1=value1,label2=value2 count,123
      // 1616413311
      //      prefix.metric2,default1=value1,default2=value2,differentdim=differentValue count,321
      // 1616413311

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
              .setDimensions(DimensionList.merge(defaultDims, labels, oneAgentDimensions))
              .setIntCounterValue(123)
              .setCurrentTime()
              .build()
              .serialize());

      System.out.println(
          Metric.builder("metric2")
              .setPrefix("prefix")
              .setDimensions(DimensionList.merge(defaultDims, differentLabels, oneAgentDimensions))
              .setIntCounterValue(321)
              .setCurrentTime()
              .build()
              .serialize());

      // output
      //      prefix.metric1,default1=value1,default2=value2,label1=value1,label2=value2 count,123
      // 1616413311
      //      prefix.metric2,default1=value1,default2=value2,differentdim=differentValue count,321
      // 1616413311
    } catch (MetricException me) {
      System.out.println(me.getMessage());
    }
  }
}
