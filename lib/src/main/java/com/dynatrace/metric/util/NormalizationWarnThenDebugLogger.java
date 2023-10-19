/**
 * Copyright 2023 Dynatrace LLC
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

import static com.dynatrace.metric.util.MetricLineConstants.ValidationMessages.*;

import java.util.logging.Level;
import java.util.logging.Logger;

class NormalizationWarnThenDebugLogger {

  private static boolean metricKeyNormalizationWarnLogged = false;
  private static boolean dimensionKeyNormalizationWarnLogged = false;
  private static boolean dimensionValueNormalizationWarnLogged = false;
  private static final String METRIC_KEY_AND_DIMENSION_KEY_TEMPLATE = "%s | %s";
  private final Logger logger;

  NormalizationWarnThenDebugLogger(Logger logger) {
    this.logger = logger;
  }

  void logMetricKeyMessage(NormalizationResult normalizationResult) {
    if (!metricKeyNormalizationWarnLogged) {
      if (logger.isLoggable(Level.WARNING)) {
        logger.warning(String.format(THROTTLE_INFO_TEMPLATE, normalizationResult.getMessage()));
      }
      metricKeyNormalizationWarnLogged = true;
    } else {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine(normalizationResult.getMessage());
      }
    }
  }

  public void logDimensionKeyMessage(String metricKey, NormalizationResult normalizationResult) {
    if (!dimensionKeyNormalizationWarnLogged) {
      if (logger.isLoggable(Level.WARNING)) {
        logger.warning(
            String.format(THROTTLE_INFO_WITH_PREFIX, metricKey, normalizationResult.getMessage()));
      }
      dimensionKeyNormalizationWarnLogged = true;
    } else {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine(String.format(PREFIX_STRING, metricKey, normalizationResult.getMessage()));
      }
    }
  }

  public void logDimensionValueMessage(
      String metricKey, String dimensionKey, NormalizationResult normalizationResult) {
    if (!dimensionValueNormalizationWarnLogged) {
      if (logger.isLoggable(Level.WARNING)) {
        String identifier =
            String.format(METRIC_KEY_AND_DIMENSION_KEY_TEMPLATE, metricKey, dimensionKey);
        logger.warning(
            String.format(THROTTLE_INFO_WITH_PREFIX, identifier, normalizationResult.getMessage()));
      }
      dimensionValueNormalizationWarnLogged = true;
    } else {
      if (logger.isLoggable(Level.FINE)) {
        String identifier =
            String.format(METRIC_KEY_AND_DIMENSION_KEY_TEMPLATE, metricKey, dimensionKey);
        logger.fine(String.format(PREFIX_STRING, identifier, normalizationResult.getMessage()));
      }
    }
  }
}
