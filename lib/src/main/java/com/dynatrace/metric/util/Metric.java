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
package com.dynatrace.metric.util;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Metric {
  private String prefix;
  private String name;
  //    private MetricValue value;
  //    private NormalizedDimensionList dimensions;
  //    private Timestamp time;

  public String serialize() {
    throw new NotImplementedException();
  }
}
