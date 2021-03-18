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

import java.util.stream.Stream;
import junit.framework.TestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NormalizeTest extends TestCase {

  public void testDimensionList() {}

  @ParameterizedTest
  @MethodSource("provideDimensionKeys")
  public void testDimensionKey(String input, String expected) {
    assertEquals(expected, Normalize.dimensionKey(input));
  }

  public void testDimensionValue() {}

  public void testMetricKey() {}

  private static Stream<Arguments> provideDimensionKeys() {
    return Stream.of(Arguments.of("key1", "key1"));
  }
}
