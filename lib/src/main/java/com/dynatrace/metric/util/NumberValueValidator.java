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

import com.dynatrace.metric.util.MetricLineConstants.ValidationMessages;

/** Offers validation methods for metric-line values */
final class NumberValueValidator {
  private static final double COMPARISON_ABSOLUTE_TOLERANCE = 0.000001D;

  private NumberValueValidator() {}

  /**
   * Checks if all constraints for summary data are valid.
   *
   * @param min The minimum of the gauge
   * @param max The maximum of the gauge
   * @param sum The sum of the gauge
   * @param count The count of the gauge
   * @return The {@link BooleanResultMessage result}, containing if the summary is valid, or not
   *     along with an error-message.
   */
  static BooleanResultMessage isSummaryValid(double min, double max, double sum, long count) {
    if (count < 0) {
      return BooleanResultMessage.newInvalid(
          () ->
              String.format(ValidationMessages.GAUGE_COUNT_NEGATIVE_MESSAGE, min, max, sum, count));
    }
    if (count == 0 && (min != 0 || max != 0 || sum != 0)) {
      return BooleanResultMessage.newInvalid(
          () -> String.format(ValidationMessages.GAUGE_COUNT_ZERO_MESSAGE, min, max, sum, count));
    } else if (count == 0) {
      return BooleanResultMessage.newValid();
    }

    if (Double.isInfinite(min) || Double.isInfinite(max) || Double.isInfinite(sum)) {
      return BooleanResultMessage.newInvalid(
          () -> String.format(ValidationMessages.GAUGE_INFINITE_MESSAGE, min, max, sum, count));
    }

    if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(sum)) {
      return BooleanResultMessage.newInvalid(
          () -> String.format(ValidationMessages.GAUGE_NAN_MESSAGE, min, max, sum, count));
    }

    if (max < min) {
      return BooleanResultMessage.newInvalid(
          () ->
              String.format(
                  ValidationMessages.GAUGE_MIN_GREATER_MAX_MESSAGE, min, max, sum, count));
    }

    double avg = sum / count;
    if (!(lessOrEqualWithAbsoluteTolerance(min, avg)
        && lessOrEqualWithAbsoluteTolerance(avg, max))) {
      // in this case the min <= avg <= max does not hold
      return BooleanResultMessage.newInvalid(
          () ->
              String.format(
                  ValidationMessages.GAUGE_INCONSISTENT_FIELDS_MESSAGE,
                  min,
                  max,
                  sum,
                  count,
                  avg,
                  COMPARISON_ABSOLUTE_TOLERANCE));
    }

    return BooleanResultMessage.newValid();
  }

  private static boolean lessOrEqualWithAbsoluteTolerance(double val1, double val2) {
    // equals
    if (Double.valueOf(val1).equals(Double.valueOf(val2))) {
      return true;
    }

    // not equal, two cases:
    // val1 < val2: should always evaluate to true
    // val1 slightly < val2 -> val1 - val2 == negative     | result <= tolerance -> true
    // val1 much < val2 -> val1 - val2 == negative         | result <= tolerance -> true
    //
    // val2 > val2: should only evaluate to true if
    // - val1 is smaller than val2, or if
    // - val1 is bigger than val2 but within the tolerance
    // val1 slightly > val2 -> val1 - val2 == small number | result <= tolerance -> true
    // val1 much > val2 -> val1 - val2 == larger number    | result > tolerance -> false
    if ((val1 - val2) <= COMPARISON_ABSOLUTE_TOLERANCE) {
      return true;
    }

    return false;
  }

  /**
   * Checks if the value of a gauge or delta of a counter is not a number, or is +/- infinity.
   *
   * @param value Either the value of a gauge, or the delta of a counter
   * @return The {@link BooleanResultMessage result}, containing if the value is valid, or not along
   *     with an error-message.
   */
  static BooleanResultMessage isValueValid(double value) {
    if (Double.isNaN(value)) {
      return BooleanResultMessage.newInvalid(() -> ValidationMessages.VALUE_NAN_MESSAGE);
    }
    if (Double.isInfinite(value)) {
      return BooleanResultMessage.newInvalid(
          () -> String.format(ValidationMessages.VALUE_INFINITE_MESSAGE, value));
    }

    return BooleanResultMessage.newValid();
  }
}
