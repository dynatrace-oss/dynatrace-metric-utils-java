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
