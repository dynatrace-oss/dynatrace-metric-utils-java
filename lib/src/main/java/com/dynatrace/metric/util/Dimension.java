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

import java.util.Objects;

/** The dimension class represents a key-value pair of strings. */
public final class Dimension {
  private final String key;
  private final String value;

  private Dimension(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /** @return A {@link String} containing the dimension key. */
  public String getKey() {
    return key;
  }

  /** @return A {@link String} containing the dimension value. */
  public String getValue() {
    return value;
  }

  /**
   * Create a new Dimension object.
   *
   * @param key the key to store.
   * @param value the value to store
   * @return a new {@link Dimension} object with the two set values.
   */
  public static Dimension create(String key, String value) {
    return new Dimension(key, value);
  }

  /**
   * @return A {@link String} containing key and value, concatenated with an equals sign (e.g.
   *     "key=value").
   */
  @Override
  public String toString() {
    return this.serialize();
  }

  String serialize() {
    return String.format("%s=%s", key, Normalize.escapeDimensionValue(value));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Dimension dimension = (Dimension) o;
    return Objects.equals(key, dimension.key) && Objects.equals(value, dimension.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}
