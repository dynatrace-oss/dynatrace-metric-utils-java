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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dynatrace.testutils.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MetadataBuilderTest {
  private static final String METRIC_NAME = "my.metric";
  private static final String GAUGE_TYPE = MetricLineConstants.PayloadGauge.GAUGE;

  @Test
  void testSetDescriptionTwice() throws MetricException {
    String expected = createExpectedLine("description2", null, null);
    assertEquals(
        expected,
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .gauge()
            .metadata()
            .description("description")
            .description("description2")
            .build());
  }

  @Test
  void testSetUnitTwice() throws MetricException {
    String expected = createExpectedLine(null, "unit2", null);
    assertEquals(
        expected,
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .gauge()
            .metadata()
            .unit("unit")
            .unit("unit2")
            .build());
  }

  @Test
  void testSetDisplayNameTwice() throws MetricException {
    String expected = createExpectedLine(null, null, "displayName2");
    assertEquals(
        expected,
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .gauge()
            .metadata()
            .displayName("displayName")
            .displayName("displayName2")
            .build());
  }

  @ParameterizedTest(
      name =
          "{index}: {0}, description: {1}, expected description: {2}, unit: {3}, expected unit: {4},  displayName: {5}, expected displayName: {6}")
  @MethodSource("provideMetadataInformation")
  void testSetVariousCombinationsOfMetadata(
      String name,
      String description,
      String expectedDescription,
      String unit,
      String expectedUnit,
      String displayName,
      String expectedDisplayName)
      throws MetricException {

    String expected = createExpectedLine(expectedDescription, expectedUnit, expectedDisplayName);
    assertEquals(
        expected,
        MetricLineBuilder.create()
            .metricKey(METRIC_NAME)
            .gauge()
            .metadata()
            .description(description)
            .unit(unit)
            .displayName(displayName)
            .build());
  }

  private static Stream<Arguments> provideMetadataInformation() {
    List<Tuple> descriptions =
        Arrays.asList(
            Tuple.of(
                "description with no normalization/escaping",
                "do_not_normalize_description",
                "do_not_normalize_description"),
            Tuple.of(
                "description to normalize/escape", "escape description", "escape\\ description"),
            Tuple.of("all invalid characters description", "\u0000", "_"),
            Tuple.of("empty description", "", null),
            Tuple.of("empty quoted description", "\"\"", null),
            Tuple.of("null description", null, null));

    List<Tuple> units =
        Arrays.asList(
            Tuple.of("valid unit", "validunit", "validunit"),
            Tuple.of("invalid unit", "{invalid unit}", null),
            Tuple.of("empty unit", "", null),
            Tuple.of("null unit", null, null));

    List<Tuple> displayNames =
        Arrays.asList(
            Tuple.of(
                "displayName with no normalization/escaping",
                "valid-display-name-no-normalization",
                "valid-display-name-no-normalization"),
            Tuple.of(
                "displayName to normalize/escape",
                "escape display name",
                "escape\\ display\\ name"),
            Tuple.of("all invalid characters displayName", "\u0000", "_"),
            Tuple.of("empty displayName", "", null),
            Tuple.of("empty quoted displayName", "\"\"", null),
            Tuple.of("null displayName", null, null));

    Arguments[] arguments = new Arguments[descriptions.size() * units.size() * displayNames.size()];
    int idx = 0;
    for (Tuple description : descriptions) {
      for (Tuple unit : units) {
        for (Tuple displayName : displayNames) {
          arguments[idx++] =
              Arguments.of(
                  String.format(
                      "%s - %s - %s", description.getName(), unit.getName(), displayName.getName()),
                  description.getInput(),
                  description.getExpectedOutput(),
                  unit.getInput(),
                  unit.getExpectedOutput(),
                  displayName.getInput(),
                  displayName.getExpectedOutput());
        }
      }
    }

    return Stream.of(arguments);
  }

  private static String createExpectedLine(String description, String unit, String displayName) {
    if (description == null && unit == null && displayName == null) {
      return null;
    }

    StringBuilder sb =
        new StringBuilder()
            .appendCodePoint(CodePoints.NUMBER_SIGN)
            .append(METRIC_NAME)
            .appendCodePoint(CodePoints.BLANK)
            .append(GAUGE_TYPE)
            .appendCodePoint(CodePoints.BLANK);

    if (description != null) {
      sb.append(MetadataLineConstants.Dimensions.DESCRIPTION_KEY)
          .appendCodePoint(CodePoints.EQUALS)
          .append(description);
    }

    if (unit != null) {
      if (description != null) {
        sb.appendCodePoint(CodePoints.COMMA);
      }

      sb.append(MetadataLineConstants.Dimensions.UNIT_KEY)
          .appendCodePoint(CodePoints.EQUALS)
          .append(unit);
    }

    if (displayName != null) {
      if (description != null || unit != null) {
        sb.appendCodePoint(CodePoints.COMMA);
      }

      sb.append(MetadataLineConstants.Dimensions.DISPLAY_NAME_KEY)
          .appendCodePoint(CodePoints.EQUALS)
          .append(displayName);
    }

    return sb.toString();
  }
}
