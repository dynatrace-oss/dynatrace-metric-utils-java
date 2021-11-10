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

import com.dynatrace.file.util.DynatraceFileBasedConfigurationProvider;
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
            .withDynatraceMetadata()
            .withPrefix("prefix")
            .build();

    try {
      // the following code will create this metric line:
      // prefix.metric1,dim2=value2,default1=value1,dim1=value1,default2=value2 count,123 1616416882
      String metricLine1 =
          metricBuilderFactory
              .newMetricBuilder("metric1")
              .setDimensions(dimensions)
              .setLongGaugeValue(123)
              .setCurrentTime()
              .serialize();

      String metricLine2 =
          metricBuilderFactory
              .newMetricBuilder("metric2")
              .setDimensions(differentDimensions)
              .setLongGaugeValue(321)
              .setCurrentTime()
              .serialize();

      System.out.println(metricLine1);
      System.out.println(metricLine2);

    } catch (MetricException me) {
      System.out.println(me);
    }

    // =============================================================================================
    // Version 2: using the Metrics.Builder directly
    // =============================================================================================
    // this approach leaves reading and merging the dimensions to the user.
    DimensionList dynatraceMetadataDimensions = DimensionList.fromDynatraceMetadata();

    try {
      String metricLine1 =
          Metric.builder("metric1")
              .setPrefix("prefix")
              .setDimensions(
                  DimensionList.merge(defaultDims, dimensions, dynatraceMetadataDimensions))
              .setLongGaugeValue(123)
              .setCurrentTime()
              .serialize();

      String metricLine2 =
          Metric.builder("metric2")
              .setPrefix("prefix")
              .setDimensions(
                  DimensionList.merge(
                      defaultDims, differentDimensions, dynatraceMetadataDimensions))
              .setLongGaugeValue(321)
              .setCurrentTime()
              .serialize();

      System.out.println(metricLine1);
      System.out.println(metricLine2);

    } catch (MetricException me) {
      System.out.println(me);
    }
  }

  // to try this out, create the endpoint.properties file, start the app, and then modify the
  // contents of the file.
  static void testFilePolling() {
    // file is at /var/lib/dynatrace/enrichment/endpoint/endpoint.properties
    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();

    int counter = 0;
    while (true) {

      String token = instance.getMetricIngestToken();
      System.out.println(String.format("=============== %d ===============", counter++));
      System.out.println("Endpoint: " + instance.getMetricIngestEndpoint());
      System.out.println(
          "Token:    "
              + token.substring(
                  0,
                  Math.min(token.length(), 32))); // 32 chars = public portion only if actual token
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("Exiting...");
      }
    }
  }
}
