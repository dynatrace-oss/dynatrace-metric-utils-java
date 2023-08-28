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

/** Constants related to metadata line creation, serialization and normalization. */
final class MetadataLineConstants {

  private MetadataLineConstants() {}

  /** Constants for metadata length limits. */
  static final class Limits {

    private Limits() {}

    // Exceeding these causes the data point to be dropped
    static final int MAX_DESCRIPTION_LENGTH = 65535;
    static final int MAX_DISPLAY_NAME_LENGTH = 300;
    static final int MAX_UNIT_LENGTH = 63;
  }

  /** Constants for Dynatrace-reserved metadata dimension keys. */
  static final class Dimensions {

    private Dimensions() {}

    static final String DISPLAY_NAME_KEY = "dt.meta.displayName";
    static final String DESCRIPTION_KEY = "dt.meta.description";
    static final String UNIT_KEY = "dt.meta.unit";
  }

  /** Validation messages concerning errors and warnings of the metadata line builder. */
  static final class ValidationMessages {

    private ValidationMessages() {}

    // warnings
    static final String UNIT_DROPPED_MESSAGE = "[%s] Unit '%s' dropped";
    static final String DESCRIPTION_DROPPED_MESSAGE = "[%s] Description '%s' dropped";
    static final String DISPLAY_NAME_DROPPED_MESSAGE = "[%s] Description '%s' dropped";
    static final String METADATA_SERIALIZATION_NOT_POSSIBLE_MESSAGE =
        "No data set to serialize the metadata for the metric '%s'";
  }
}
