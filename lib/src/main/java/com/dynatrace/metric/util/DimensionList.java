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

import java.util.*;
import java.util.logging.Logger;

/** An immutable list of normalized {@link Dimension Dimensions}. */
public final class DimensionList {
  private static final Logger logger = Logger.getLogger(DimensionList.class.getName());

  private final List<Dimension> dimensions;

  private DimensionList(List<Dimension> dimensions) {
    this.dimensions = dimensions;
  }

  /**
   * Create a new {@link DimensionList} from a {@link Collection} of {@link Dimension Dimensions}.
   * All {@link DimensionList DimensionLists} must be normalized, therefore any collection passed
   * here will be normalized before storing it. Normalization does not remove duplicates.
   * Normalization will drop invalid {@link Dimension} objects.
   *
   * @param dimensions A collecion of {@link Dimension} objects, to be normalized and stored.
   * @return A {@link DimensionList} object, containing normalized {@link Dimension Dimensions}. Can
   *     still contain duplicate keys but no invalid {@link Dimension Dimensions}.
   */
  public static DimensionList fromCollection(Collection<Dimension> dimensions) {
    return new DimensionList(Normalize.dimensionList(dimensions));
  }

  /**
   * Create a new {@link DimensionList} object. Calls to {@link #fromCollection} under the hood,
   * ensuring that passed dimensions are normalized.
   *
   * @param dimensions An arbitrary number of {@link Dimension} objects.
   * @return A {@link DimensionList} object, containing normalized {@link Dimension Dimensions}. Can
   *     still contain duplicate keys.
   */
  public static DimensionList create(Dimension... dimensions) {
    return DimensionList.fromCollection(Arrays.asList(dimensions));
  }

  /**
   * Create a {@link DimensionList} from OneAgent metadata. The metadata is read automatically,
   * normalized, and stored in the resulting {@link DimensionList}. Use the returned list in a
   * {@link #merge} method or a {@link MetricBuilderFactory}.
   *
   * <p>Forwarding to {@link DimensionList#fromDynatraceMetadata()}.
   *
   * @return The result of {@link DimensionList#fromDynatraceMetadata()}.
   * @deprecated Use {@link DimensionList#fromDynatraceMetadata()} instead.
   */
  @Deprecated
  public static DimensionList fromOneAgentMetadata() {
    return fromDynatraceMetadata();
  }

  /**
   * Create a {@link DimensionList} from Dynatrace metadata. The metadata is read automatically,
   * normalized, and stored in the resulting {@link DimensionList}. Use the returned list in a
   * {@link #merge} method or a {@link MetricBuilderFactory}.
   *
   * @return A list of normalized Dynatrace metadata dimensions.
   */
  public static DimensionList fromDynatraceMetadata() {
    return DimensionList.fromCollection(DynatraceMetadataEnricher.getDynatraceMetadata());
  }

  /**
   * Check if the {@link DimensionList} contains no elements.
   *
   * @return true if the list is empty and false otherwise.
   */
  public boolean isEmpty() {
    return dimensions.isEmpty();
  }

  /**
   * Merge one or more {@link DimensionList} objects into one and remove duplicate keys. The order
   * of the passed lists matters, {@link Dimension Dimensions} of {@link DimensionList
   * DimensionLists} passed further right will overwrite {@link Dimension Dimensions} of {@link
   * DimensionList DimensionLists} passed further left if they share the same key (after key
   * normalization). Similarly, {@link Dimension Dimensions} in the same {@link DimensionList} that
   * share a key will be overwritten by {@link Dimension Dimensions} in the same {@link
   * DimensionList} with the same key. When passing only one list, removes duplicates from the list.
   *
   * @param dimensionLists One or more {@link DimensionList} objects.
   * @return A new {@link DimensionList} object containing unique {@link Dimension Dimensions} from
   *     all passed lists.
   */
  public static DimensionList merge(DimensionList... dimensionLists) {
    if (dimensionLists == null) {
      return DimensionList.create();
    }

    Map<String, Dimension> dimensionMap = new HashMap<>();
    for (DimensionList dl : dimensionLists) {
      if (dl == null) {
        continue;
      }
      // overwrite dimension keys with items that are passed further right.
      for (Dimension dimension : dl.dimensions) {
        if (dimension.getKey() == null || dimension.getKey().isEmpty()) {
          logger.warning("skipping empty key");
          continue;
        }
        dimensionMap.put(dimension.getKey(), dimension);
      }
    }
    return DimensionList.fromCollection(dimensionMap.values());
  }

  /**
   * Access the elements in the {@link DimensionList}.
   *
   * @return An unmodifiable {@link Collection} of {@link Dimension} objects.
   */
  public Collection<Dimension> getDimensions() {
    return Collections.unmodifiableList(dimensions);
  }

  String serialize() {
    if (dimensions.isEmpty()) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    boolean firstIteration = true;

    for (Dimension dimension : dimensions) {
      // if the dimension is not valid, don't add it to the
      if (isDimensionValid(dimension)) {
        if (!firstIteration) {
          builder.append(",");
        } else {
          firstIteration = false;
        }

        builder.append(dimension.serialize());
      }
    }

    return builder.toString();
  }

  static boolean isDimensionValid(Dimension dimension) {
    String key = dimension.getKey();
    if (key == null || key.isEmpty()) {
      logger.warning("dimension key is null or empty.");
      return false;
    }

    String value = dimension.getValue();
    if (value == null || value.isEmpty()) {
      logger.warning(() -> String.format("dimension value for dimension key '%s' is null or empty.", key));
      return false;
    }

    return true;
  }
}
