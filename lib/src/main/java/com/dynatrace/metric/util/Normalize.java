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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class Normalize {
  private static final Logger logger = Logger.getLogger(Normalize.class.getName());

  static List<Dimension> dimensionList(List<Dimension> dimensions) {
    List<Dimension> normalized = new ArrayList<>();
    for (Dimension dimension : dimensions) {
      try {
        String key = dimensionKey(dimension.Key);
        normalized.add(Dimension.create(key, dimensionValue(dimension.Value)));
      } catch (IllegalArgumentException iae) {
        logger.warning(String.format("could not normalize dimension key: '%s'", dimension.Key));
      }
    }
    return normalized;
  }

  static String dimensionKey(String key) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(key)) {
      throw new IllegalArgumentException("key cannot be null or empty");
    }
    // todo: normalization
    return key;
  }

  static String dimensionValue(String value) {
    // todo
    return value;
  }
}
