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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NumberValueValidatorTest {
  @Test
  void testValueValidator() {
    assertTrue(NumberValueValidator.isValueValid(0).isValid());
    assertTrue(NumberValueValidator.isValueValid(100.1).isValid());
    assertTrue(NumberValueValidator.isValueValid(-100.9).isValid());
    assertFalse(NumberValueValidator.isValueValid(Double.NaN).isValid());
    assertFalse(NumberValueValidator.isValueValid(Double.NEGATIVE_INFINITY).isValid());
    assertFalse(NumberValueValidator.isValueValid(Double.POSITIVE_INFINITY).isValid());
  }

  @Test
  void testInconsistentGaugeFields() {
    // min > avg
    assertFalse(NumberValueValidator.isSummaryValid(2.5, 3, 6, 3).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(5, 3, 3, 1).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(-2, -2, -5, 2).isValid());

    // max < avg
    assertFalse(NumberValueValidator.isSummaryValid(2, 2, 5, 3).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(10.5, 10.6, 21.3, 1).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(-5.3, -4.1, -5, 2).isValid());

    // test tolerances (tolerance == 0.000001);
    // avg == 2; min <= avg does not hold (but is within tolerance).
    assertTrue(NumberValueValidator.isSummaryValid(2.0000001, 3, 4, 2).isValid());
    // avg == -2; min <= avg does not hold (but is within tolerance)
    assertTrue(NumberValueValidator.isSummaryValid(-1.999999, -1, -4, 2).isValid());

    // avg == 2; max >= avg does not hold (but is within tolerance)
    assertTrue(NumberValueValidator.isSummaryValid(1.5, 1.999999, 4, 2).isValid());
    // avg == -2; max >= avg does not hold (but is within tolerance)
    assertTrue(NumberValueValidator.isSummaryValid(-2.5, -2.0000001, -4, 2).isValid());
  }

  @Test
  void testSummaryValidator() {
    assertTrue(() -> NumberValueValidator.isSummaryValid(-1.3, 4.1, 5, 2).isValid());

    assertTrue(NumberValueValidator.isSummaryValid(0, 0, 0, 0).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(0, 0, 0, -1).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(0, 0, 1, 0).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(0, 1, 0, 0).isValid());
    assertFalse(NumberValueValidator.isSummaryValid(1, 0, 0, 0).isValid());

    assertFalse(NumberValueValidator.isSummaryValid(1.1, 1, 1, 1).isValid());

    Double validValue = 1.23d;
    List<Double> values =
        Arrays.asList(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, validValue);
    for (Double minVal : values) {
      for (Double maxVal : values) {
        for (Double sumVal : values) {
          if (minVal.equals(validValue) && maxVal.equals(validValue) && sumVal.equals(validValue)) {
            // skip the case where min, max, and count are the valid number. All other combinations
            // where at least one of the values is invalid should throw.
            continue;
          }

          assertFalse(NumberValueValidator.isSummaryValid(minVal, maxVal, sumVal, 1).isValid());
        }
      }
    }
  }
}
