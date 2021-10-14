package com.dynatrace.file.util;

import com.dynatrace.metric.util.DynatraceMetricApiConstants;

public class DynatraceConfiguration {
    private String endpoint = DynatraceMetricApiConstants.getDefaultOneAgentEndpoint();
    private String token = "";

    public String getToken() {
        return token;
    }

    void setToken(String token) {
        this.token = token;
    }

    public String getEndpoint() {
        return endpoint;
    }

    void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
