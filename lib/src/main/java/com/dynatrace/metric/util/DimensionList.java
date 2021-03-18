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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DimensionList {
  private static final Logger logger = Logger.getLogger(Dimension.class.getName());

  private Collection<Dimension> dimensions;

  private DimensionList(Collection<Dimension> dimensions) {
    this.dimensions = dimensions;
  }

  public static DimensionList create(Dimension... dimensions) {
    return new DimensionList(Normalize.dimensionList(Arrays.asList(dimensions)));
  }

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
    return new DimensionList(dimensionMap.values());
  }

  public Collection<Dimension> getDimensions() {
    return dimensions;
  }
}
