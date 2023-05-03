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

public final class DynatraceMetricApiConstants {
  private DynatraceMetricApiConstants() {}

  private static final String DEFAULT_ONEAGENT_ENDPOINT = "http://localhost:14499/metrics/ingest";
  private static final int PAYLOAD_LINES_LIMIT = 1000;

  /**
   * @return The default OneAgent endpoint. See the <a
   *     href="https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/ingestion-methods/local-api/">Dynatrace
   *     documentation</a> for more information.
   */
  public static String getDefaultOneAgentEndpoint() {
    return DEFAULT_ONEAGENT_ENDPOINT;
  }

  /**
   * @return The maximum number of metric lines per request accepted by the ingest endpoint.
   */
  public static int getPayloadLinesLimit() {
    return PAYLOAD_LINES_LIMIT;
  }
}
