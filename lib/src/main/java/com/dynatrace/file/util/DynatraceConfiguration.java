/**
 * Copyright 2022 Dynatrace LLC
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

package com.dynatrace.file.util;

import com.dynatrace.metric.util.DynatraceMetricApiConstants;

class DynatraceConfiguration {
  private String metricIngestEndpoint = DynatraceMetricApiConstants.getDefaultOneAgentEndpoint();
  private String metricIngestToken = "";

  public String getMetricIngestToken() {
    return metricIngestToken;
  }

  void setMetricIngestToken(String metricIngestToken) {
    this.metricIngestToken = metricIngestToken;
  }

  public String getMetricIngestEndpoint() {
    return metricIngestEndpoint;
  }

  void setMetricIngestEndpoint(String metricIngestEndpoint) {
    this.metricIngestEndpoint = metricIngestEndpoint;
  }
}
