/**
 * Copyright 2026 Dynatrace LLC
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

import java.util.Map;

/** Gives tests in other packages access to the package-private test hooks of the library. */
public final class GoldenFileTestSupport {
  private GoldenFileTestSupport() {}

  /**
   * Enables the Dynatrace metadata dimensions and serves them from the given map instead of the
   * Dynatrace metadata files.
   */
  public static MetricLinePreConfiguration.Builder withDynatraceMetadata(
      MetricLinePreConfiguration.Builder builder, Map<String, String> metadata) {
    return builder.dynatraceMetadataSource(metadata).dynatraceMetadataDimensions();
  }
}
