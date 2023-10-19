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
