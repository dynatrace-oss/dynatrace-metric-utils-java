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

import static org.mockito.Mockito.*;

import java.util.logging.Level;
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

  private final Logger mockLogger = mock(Logger.class);;
  private NormalizationWarnThenDebugLogger warnThenDebugLogger;

  @BeforeEach
  void beforeEach() {
    reset(mockLogger);
    // In order to verify success in the tests, log records at any log level.
    when(mockLogger.isLoggable(any(Level.class))).thenReturn(true);

    warnThenDebugLogger = new NormalizationWarnThenDebugLogger(mockLogger);
  }

  @Test
  void logMetricKeyMessage_logsWarnThenDebug() {
    // should be logged at warn
    warnThenDebugLogger.logMetricKeyMessage(RESULT_1);
    // should be logged at fine
    warnThenDebugLogger.logMetricKeyMessage(RESULT_2);

    verify(mockLogger, atLeastOnce()).isLoggable(Level.WARNING);
    verify(mockLogger, atLeastOnce()).isLoggable(Level.FINE);
    verify(mockLogger)
        .warning(
            RESULT_1.getMessage()
                + ". Further normalization logs for data of the same type will be logged at debug level.");
    verify(mockLogger).fine(RESULT_2.getMessage());
    verifyNoMoreInteractions(mockLogger);
  }

  @Test
  void logDimensionKeyMessage_logsWarnThenDebug() {
    warnThenDebugLogger.logDimensionKeyMessage(METRIC_KEY_1, RESULT_1);
    warnThenDebugLogger.logDimensionKeyMessage(METRIC_KEY_1, RESULT_2);
    warnThenDebugLogger.logDimensionKeyMessage(METRIC_KEY_2, RESULT_1);

    verify(mockLogger, atLeastOnce()).isLoggable(Level.WARNING);
    verify(mockLogger, atLeast(2)).isLoggable(Level.FINE);
    // first message is logged at warn with the additional info that further logs will be logged at
    // debug.
    verify(mockLogger)
        .warning(
            String.format(
                "[%s] %s. %s",
                METRIC_KEY_1,
                RESULT_1.getMessage(),
                "Further normalization logs for data of the same type will be logged at debug level."));
    // all further messages are logged at debug level, independent of the metric key or message.
    verify(mockLogger).fine(String.format("[%s] %s", METRIC_KEY_1, RESULT_2.getMessage()));
    verify(mockLogger).fine(String.format("[%s] %s", METRIC_KEY_2, RESULT_1.getMessage()));
    verifyNoMoreInteractions(mockLogger);
  }

  @Test
  void logDimensionValueMessage_logsWarnThenDebug() {
    warnThenDebugLogger.logDimensionValueMessage(METRIC_KEY_1, DIMENSION_KEY_1, RESULT_1);
    warnThenDebugLogger.logDimensionValueMessage(METRIC_KEY_1, DIMENSION_KEY_1, RESULT_2);
    warnThenDebugLogger.logDimensionValueMessage(METRIC_KEY_2, DIMENSION_KEY_2, RESULT_1);

    verify(mockLogger, atLeastOnce()).isLoggable(Level.WARNING);
    verify(mockLogger, atLeast(2)).isLoggable(Level.FINE);
    // first message is logged at warn with the additional info that further logs will be logged at
    // debug.
    verify(mockLogger)
        .warning(
            String.format(
                "[%s | %s] %s. %s",
                METRIC_KEY_1,
                DIMENSION_KEY_1,
                RESULT_1.getMessage(),
                "Further normalization logs for data of the same type will be logged at debug level."));
    // all further messages are logged at debug level, independent of the metric key or message.
    verify(mockLogger)
        .fine(String.format("[%s | %s] %s", METRIC_KEY_1, DIMENSION_KEY_1, RESULT_2.getMessage()));
    verify(mockLogger)
        .fine(String.format("[%s | %s] %s", METRIC_KEY_2, DIMENSION_KEY_2, RESULT_1.getMessage()));
    verifyNoMoreInteractions(mockLogger);
  }
}
