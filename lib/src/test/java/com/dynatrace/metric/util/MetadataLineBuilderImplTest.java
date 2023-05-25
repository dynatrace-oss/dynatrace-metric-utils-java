package com.dynatrace.metric.util;

import org.junit.jupiter.api.Test;
import sun.jvm.hotspot.oops.Metadata;

import static org.junit.jupiter.api.Assertions.*;
class MetadataLineBuilderImplTest {


  @Test
  void shouldNotEscapeSimpleDescription() {
    String line = MetadataLineBuilderImpl.newBuilder()
      .metricKey("my.metric")
      .counter()
      .description("description")
      .unit("count")
      .build();

    assertEquals("#my.metric count dt.meta.description=description,dt.meta.unit=count", line);
  }

  @Test
  void testShouldEscapeSpacesInDimensionValues() {
    String line = MetadataLineBuilderImpl.newBuilder()
      .metricKey("my.metric")
      .counter()
      .description("some description")
      .unit("count")
      .build();

    assertEquals("#my.metric count dt.meta.description=some\\ description,dt.meta.unit=count", line);
  }

}