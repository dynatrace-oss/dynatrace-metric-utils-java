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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DimensionTest {
  @Test
  void serialize() {
    assertEquals("=", Dimension.create("", "").serialize());
    assertEquals("=value", Dimension.create("", "value").serialize());
    assertEquals("key=value", Dimension.create("key", "value").serialize());
    assertEquals("~!@#$=~@#$", Dimension.create("~!@#$", "~@#$").serialize());
  }
}
