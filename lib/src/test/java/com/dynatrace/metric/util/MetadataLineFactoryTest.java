package com.dynatrace.metric.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class MetadataLineFactoryTest {
  private static final String METRIC_NAME = "my.metric";
  private static final String COUNT_TYPE = MetricType.COUNTER.toString();
  private static final String GAUGE_TYPE = MetricType.GAUGE.toString();


  // todo test what happens if you pass some other type to the metadata line creation
  // todo test how units with a space can appear when description is also set.
  @Test
  void whenUnitAndDescriptionAreEmpty_shouldReturnNull() {
    assertNull(MetadataLineFactory.createMetadataLine(METRIC_NAME, null, null, COUNT_TYPE));
    assertNull(MetadataLineFactory.createMetadataLine(METRIC_NAME, null, null, GAUGE_TYPE));
  }


  @Test
  void shouldNotEscapeSimpleDescription() {
//    String line =
//        MetadataLineFactory.createCounterMetadataLine("my.metric", "description", "count");
//      MetadataLineFactory.createMetadataLine(METRIC_NAME, )

//    assertEquals("#my.metric count dt.meta.description=description,dt.meta.unit=count", line);
  }

  @Test
  void testShouldEscapeSpacesInDimensionValues() {
//    String line =
//        MetadataLineFactory.createCounterMetadataLine("my.metric", "some description", "count");
//
//    assertEquals(
//        "#my.metric count dt.meta.description=some\\ description,dt.meta.unit=count", line);
  }
}
