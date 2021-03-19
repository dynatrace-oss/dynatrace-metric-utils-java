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
import java.util.*;
import java.util.logging.Logger;

public class DimensionList {
  private static final Logger logger = Logger.getLogger(Dimension.class.getName());

  private final List<Dimension> dimensions;

  private DimensionList(List<Dimension> dimensions) {
    this.dimensions = dimensions;
  }

  /**
   * Create a new {@link DimensionList} object. All {@link DimensionList DimensionLists} must be
   * normalized. This function will automatically normalize all dimensions passed to it, and discard
   * Dimensions with invalid keys.
   *
   * @param dimensions An arbitrary number of {@link Dimension} objects.
   * @return A {@link DimensionList} object only containing valid dimensions. Might still contain
   *     duplicates.
   */
  public static DimensionList create(Dimension... dimensions) {
    return new DimensionList(Normalize.dimensionList(Arrays.asList(dimensions)));
  }

  /**
   * Merge one or more {@link DimensionList} objects into one and remove duplicate keys. The order
   * of the passed lists matters, {@link Dimension Dimensions} of {@link DimensionList
   * DimensionLists} passed further right will overwrite {@link Dimension Dimensions} of {@link
   * DimensionList DimensionLists} passed further left if they share the same key (after key
   * normalization). Similarly, {@link Dimension Dimensions} in the same {@link DimensionList} that
   * share a key will be overwritten by {@link Dimension Dimensions} in the same {@link
   * DimensionList} if they share a key.
   *
   * @param dimensionLists One or more {@link DimensionList} objects.
   * @return A new {@link DimensionList} object containing unique {@link Dimension Dimensions} from
   *     all passed lists.
   */
  public static DimensionList merge(DimensionList... dimensionLists) {
    Map<String, Dimension> dimensionMap = new HashMap<>();
    for (DimensionList dl : dimensionLists) {
      // overwrite dimension keys with items that are passed further right.
      for (Dimension dimension : dl.dimensions) {
        if (Strings.isNullOrEmpty(dimension.Key)) {
          logger.warning("skipping empty key");
          continue;
        }
        dimensionMap.put(dimension.Key, dimension);
      }
    }
    return new DimensionList(new ArrayList<>(dimensionMap.values()));
  }

  public Collection<Dimension> getDimensions() {
    return dimensions;
  }

  String serialize() {
    if (dimensions.isEmpty()) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    boolean firstIteration = true;

    for (Dimension dimension : dimensions) {
      if (!firstIteration) {
        builder.append(",");
      } else {
        firstIteration = false;
      }

      builder.append(dimension.serialize());
    }

    return builder.toString();
  }
}
