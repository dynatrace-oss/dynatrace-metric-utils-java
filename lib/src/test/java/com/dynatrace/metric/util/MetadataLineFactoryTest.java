package com.dynatrace.metric.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MetadataLineFactoryTest {

  @Test
  void shouldNotEscapeSimpleDescription() {
    String line =
        MetadataLineFactory.createCounterMetadataLine("my.metric", "description", "count");

    assertEquals("#my.metric count dt.meta.description=description,dt.meta.unit=count", line);
  }

  @Test
  void testShouldEscapeSpacesInDimensionValues() {
    String line =
        MetadataLineFactory.createCounterMetadataLine("my.metric", "some description", "count");

    assertEquals(
        "#my.metric count dt.meta.description=some\\ description,dt.meta.unit=count", line);
  }
}
