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

import com.dynatrace.metric.util.Dimension;
import com.dynatrace.metric.util.DimensionList;
import com.dynatrace.metric.util.Metric;
import com.dynatrace.metric.util.MetricValues;

public class App {
    public static void main(String[] args) {

        // DimensionList.create will automatically call normalize for each of the dimensions.
        // the first two can be created once, and then passed to the merge function for each
        // new created metric. That way they dont have to be normalized every time.
        // The main question here is if we want a kind of factory, that we can pass these two
        // lists to, and which will create metric objects given just a set of labels (but
        // on the back end does exactly what is shown here).
        DimensionList defaultDims = DimensionList.create(
                Dimension.create("default1", "value1"),
                Dimension.create("default2", "value2")
        );

        DimensionList oneAgentData = DimensionList.create(
                Dimension.create("one1", "value1"),
                Dimension.create("one2", "value2")
        );

        DimensionList labels = DimensionList.create(
                Dimension.create("label1", "value1"),
                Dimension.create("label2", "value2")
        );

        // dimensions in lists further right will overwrite dimensions in items further left.
        // this will have to be done for each metric, as each metric can have different labels.
        DimensionList merged = DimensionList.merge(defaultDims, labels, oneAgentData);
        // create the metric
        Metric metric = Metric
                .builder("name")
                .setPrefix("prefix")
                .setDimensions(merged)
                .setCurrentTime()
                .setIntCounterValue(32)
                .build();

        // and transform it to a string
        System.out.println(metric.serialize());
        
        for (Dimension dim:merged.getDimensions()){
            System.out.println(dim);
        }
    }
}
