package com.dynatrace.metric.util;

/** Constants related to metadata line creation, serialization and normalization. */
final class MetadataConstants {

  private MetadataConstants() {}

  /** Constants for metadata length. */
  static final class Limits {

    private Limits() {}

    static final int MAX_DESCRIPTION_LENGTH = 65535;
    static final int MAX_UNIT_LENGTH = 63;
  }

  /** Constants for Dynatrace-reserved metadata dimension keys */
  static final class Dimensions {

    private Dimensions() {}

    static final String DESCRIPTION_KEY = "dt.meta.description";
    static final String UNIT_KEY = "dt.meta.unit";
  }
}
