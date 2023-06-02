package com.dynatrace.metric.util;

/** Constants related to metadata line creation, serialization and normalization. */
public final class MetadataConstants {

  private MetadataConstants() {}

  /** Constants for metadata length limits according to . */
  public static final class Limits {

    private Limits() {}

    public static final int MAX_DESCRIPTION_LENGTH = 65535;
    public static final int MAX_UNIT_LENGTH = 63;
  }

  /** Constants for Dynatrace-reserved metadata dimension keys */
  public static final class Dimensions {

    private Dimensions() {}

    public static final String DESCRIPTION_KEY = "dt.meta.description";
    public static final String UNIT_KEY = "dt.meta.unit";
  }
}
