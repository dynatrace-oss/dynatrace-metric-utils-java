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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalizationWarnThenDebugLoggerTest {
  private static final NormalizationResult RESULT_1 =
      NormalizationResult.newWarning("result1", () -> "first warning message");
  private static final NormalizationResult RESULT_2 =
      NormalizationResult.newWarning("result2", () -> "second warning message");
  private static final String METRIC_KEY_1 = "metric.key.first";
  private static final String METRIC_KEY_2 = "metric.key.second";
  private static final String DIMENSION_KEY_1 = "dimension.key.first";
  private static final String DIMENSION_KEY_2 = "dimension.key.second";
  private static final String THROTTLE_INFO =
      "Further normalization logs for data of the same type will be logged at debug level.";

  // records every log call as "LEVEL: message"
  private final List<String> logged = new ArrayList<>();
  private NormalizationWarnThenDebugLogger warnThenDebugLogger;

  @BeforeEach
  void beforeEach() {
    Logger logger = Logger.getAnonymousLogger();
    logger.setUseParentHandlers(false);
    // In order to verify success in the tests, log records at any log level.
    logger.setLevel(Level.ALL);
    logger.addHandler(
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            logged.add(record.getLevel() + ": " + record.getMessage());
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        });

    warnThenDebugLogger = new NormalizationWarnThenDebugLogger(logger);
  }

  @Test
  void logMetricKeyMessage_logsWarnThenDebug() {
    // should be logged at warn
    warnThenDebugLogger.logMetricKeyMessage(RESULT_1);
    // should be logged at fine
    warnThenDebugLogger.logMetricKeyMessage(RESULT_2);

    assertEquals(
        Arrays.asList(
            "WARNING: " + RESULT_1.getMessage() + ". " + THROTTLE_INFO,
            "FINE: " + RESULT_2.getMessage()),
        logged);
  }

  @Test
  void logDimensionKeyMessage_logsWarnThenDebug() {
    warnThenDebugLogger.logDimensionKeyMessage(METRIC_KEY_1, RESULT_1);
    warnThenDebugLogger.logDimensionKeyMessage(METRIC_KEY_1, RESULT_2);
    warnThenDebugLogger.logDimensionKeyMessage(METRIC_KEY_2, RESULT_1);

    // first message is logged at warn with the additional info that further logs will be logged at
    // debug. All further messages are logged at debug level, independent of the metric key or
    // message.
    assertEquals(
        Arrays.asList(
            String.format(
                "WARNING: [%s] %s. %s", METRIC_KEY_1, RESULT_1.getMessage(), THROTTLE_INFO),
            String.format("FINE: [%s] %s", METRIC_KEY_1, RESULT_2.getMessage()),
            String.format("FINE: [%s] %s", METRIC_KEY_2, RESULT_1.getMessage())),
        logged);
  }

  @Test
  void logDimensionValueMessage_logsWarnThenDebug() {
    warnThenDebugLogger.logDimensionValueMessage(METRIC_KEY_1, DIMENSION_KEY_1, RESULT_1);
    warnThenDebugLogger.logDimensionValueMessage(METRIC_KEY_1, DIMENSION_KEY_1, RESULT_2);
    warnThenDebugLogger.logDimensionValueMessage(METRIC_KEY_2, DIMENSION_KEY_2, RESULT_1);

    // first message is logged at warn with the additional info that further logs will be logged at
    // debug. All further messages are logged at debug level, independent of the metric key or
    // message.
    assertEquals(
        Arrays.asList(
            String.format(
                "WARNING: [%s | %s] %s. %s",
                METRIC_KEY_1, DIMENSION_KEY_1, RESULT_1.getMessage(), THROTTLE_INFO),
            String.format(
                "FINE: [%s | %s] %s", METRIC_KEY_1, DIMENSION_KEY_1, RESULT_2.getMessage()),
            String.format(
                "FINE: [%s | %s] %s", METRIC_KEY_2, DIMENSION_KEY_2, RESULT_1.getMessage())),
        logged);
  }

  @Test
  void logMetadataMessage_logsWarnThenDebug() {
    warnThenDebugLogger.logMetadataMessage(METRIC_KEY_1, RESULT_1);
    warnThenDebugLogger.logMetadataMessage(METRIC_KEY_1, RESULT_2);
    warnThenDebugLogger.logMetadataMessage(METRIC_KEY_2, RESULT_1);

    assertEquals(
        Arrays.asList(
            String.format(
                "WARNING: [%s] %s. %s", METRIC_KEY_1, RESULT_1.getMessage(), THROTTLE_INFO),
            String.format("FINE: [%s] %s", METRIC_KEY_1, RESULT_2.getMessage()),
            String.format("FINE: [%s] %s", METRIC_KEY_2, RESULT_1.getMessage())),
        logged);
  }
}
