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
import com.dynatrace.metric.util.MetricException;
import com.dynatrace.metric.util.MetricLineBuilder;
import com.dynatrace.metric.util.MetricLinePreConfiguration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class App {
  public static void main(String[] args) {
    Map<String, String> defaultDims = new HashMap<>();
    defaultDims.put("default1", "value1");
    defaultDims.put("default2", "value2");

    Map<String, String> dimensions = new HashMap<>();
    dimensions.put("dim1", "value1");
    dimensions.put("dim2", "value2");

    Map<String, String> differentDimensions =
        Collections.singletonMap("differentDim", "differentValue");

    try {

      // =============================================================================================
      // Version 1: using the MetricLineBuilder with MetricLinePreConfiguration
      // =============================================================================================
      // setup metric line pre-configuration with items that can be shared between multiple metrics
      MetricLinePreConfiguration preConfig =
          MetricLinePreConfiguration.builder()
              .defaultDimensions(defaultDims)
              .dynatraceMetadataDimensions()
              .prefix("prefix")
              .build();

      // the following code will create this metric line:
      // prefix.metric1,default1=value1,default2=value2,dim2=value2,dim1=value1 gauge,123
      // <timestamp>
      String metricLine1 =
          MetricLineBuilder.create(preConfig)
              .metricKey("metric1")
              .dimensions(dimensions)
              .gauge()
              .value(123)
              .timestamp(Instant.now())
              .build();

      // the following code will create this metric line:
      // prefix.metric2,default1=value1,default2=value2,differentdim=differentValue gauge,321
      // <timestamp>
      String metricLine2 =
          MetricLineBuilder.create(preConfig)
              .metricKey("metric2")
              .dimensions(differentDimensions)
              .gauge()
              .value(321)
              .timestamp(Instant.now())
              .build();

      System.out.println(metricLine1);
      System.out.println(metricLine2);

    } catch (MetricException me) {
      System.out.println(me);
    }

    // =============================================================================================
    // Version 2: using the MetricLineBuilder without MetricLinePreConfiguration
    // =============================================================================================
    // this approach leaves using shared attributes out

    try {
      // the following code will create this metric line:
      // metric1,dim2=value2,dim1=value1 gauge,123 <timestamp>
      String metricLine1 =
          MetricLineBuilder.create()
              .metricKey("metric1")
              .dimensions(dimensions)
              .gauge()
              .value(123)
              .timestamp(Instant.now())
              .build();

      // the following code will create this metric line:
      // metric2,differentdim=differentValue gauge,321 <timestamp>
      String metricLine2 =
          MetricLineBuilder.create()
              .metricKey("metric2")
              .dimensions(differentDimensions)
              .gauge()
              .value(321)
              .timestamp(Instant.now())
              .build();

      System.out.println(metricLine1);
      System.out.println(metricLine2);

    } catch (MetricException me) {
      System.out.println(me);
    }

    // =============================================================================================
    // Metadata
    // =============================================================================================
    try {
      MetricLineBuilder.GaugeStep gaugeBuilder =
          MetricLineBuilder.create().metricKey("metric1").gauge();

      MetricLineBuilder.CounterStep countBuilder =
          MetricLineBuilder.create().metricKey("metric1").count();

      // the following code will create this metric line:
      // metric1 gauge,321
      // #metric1 gauge dt.meta.description=A\ description\ of\ the\ metric,dt.meta.unit=unit
      String metricLine1 = gaugeBuilder.value(321).build();
      String metadataLine1 =
          gaugeBuilder.metadata().unit("unit").description("A description of the metric").build();

      // the following code will create this metric line:
      // metric1 count,delta=321
      // #metric1 count dt.meta.description=This\ metric\ measures\ something\ in\
      // Bytes,dt.meta.unit=Byte
      String metricLine2 = countBuilder.delta(321).build();
      String metadataLine2 =
          countBuilder
              .metadata()
              .unit("Byte")
              .description("This metric measures something in Bytes")
              .build();

      System.out.println(metricLine1);
      System.out.println(metadataLine1);
      System.out.println(metricLine2);
      System.out.println(metadataLine2);

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
