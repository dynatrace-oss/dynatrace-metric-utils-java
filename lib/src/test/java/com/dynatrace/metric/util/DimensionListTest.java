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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DimensionListTest {
  private static void assertUnorderedEquals(
      Collection<Dimension> expected, Collection<Dimension> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
    assertTrue(actual.containsAll(expected));
  }

  @Test
  void createEmpty() {
    Dimension[] dimensions = {};

    Collection<Dimension> expected = Arrays.asList(dimensions);
    Collection<Dimension> actual = DimensionList.create(dimensions).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void createValid() {
    Dimension[] dimensions = {
      Dimension.create("valid1", "value1"), Dimension.create("valid2", "value2")
    };

    Collection<Dimension> expected = Arrays.asList(dimensions);
    Collection<Dimension> actual = DimensionList.create(dimensions).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void createWithNormalization() {
    Dimension[] dimensions = {
      Dimension.create("~!@LEADING", "value1"), Dimension.create("TRAILING!@#", "value2")
    };

    Collection<Dimension> expected =
        Arrays.asList(
            Dimension.create("_leading", "value1"), Dimension.create("trailing_", "value2"));

    Collection<Dimension> actual = DimensionList.create(dimensions).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void createWithNormalizationRemoveInvalidKey() {
    Collection<Dimension> expected =
        Arrays.asList(
            Dimension.create("valid", "value"),
            Dimension.create("_", "underscore"),
            Dimension.create("another_valid", "value"));

    Dimension[] dimensions = {
      Dimension.create("valid", "value"),
      Dimension.create("!@3@$", "underscore"),
      Dimension.create("another_valid", "value")
    };

    Collection<Dimension> actual = DimensionList.create(dimensions).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void createRetainDuplicates() {
    Collection<Dimension> expected =
        Arrays.asList(
            Dimension.create("valid", "value1"),
            Dimension.create("valid", "value2"),
            Dimension.create("valid", "value3"));

    Dimension[] dimensions = {
      Dimension.create("valid", "value1"),
      Dimension.create("VALID", "value2"),
      Dimension.create("valid", "value3")
    };

    Collection<Dimension> actual = DimensionList.create(dimensions).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void mergeEmpty() {
    DimensionList dl1 = DimensionList.create();
    DimensionList dl2 = DimensionList.create();
    DimensionList dl3 = DimensionList.create();

    Collection<Dimension> expected = new ArrayList<>();
    Collection<Dimension> actual = DimensionList.merge(dl1, dl2, dl3).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void mergeNull() {
    DimensionList dl1 = null;
    DimensionList dl2 = null;
    DimensionList dl3 = null;

    Collection<Dimension> expected = new ArrayList<>();
    Collection<Dimension> actual = DimensionList.merge(dl1, dl2, dl3).getDimensions();

    assertEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void mergeMultipleDimensionListsNoCollision() {
    DimensionList dl1 = DimensionList.create(Dimension.create("default", "dimension"));
    DimensionList dl2 = DimensionList.create(Dimension.create("label", "dimension"));
    DimensionList dl3 = DimensionList.create(Dimension.create("overwriting", "dimension"));

    Collection<Dimension> expected =
        Arrays.asList(
            Dimension.create("default", "dimension"),
            Dimension.create("label", "dimension"),
            Dimension.create("overwriting", "dimension"));
    Collection<Dimension> actual = DimensionList.merge(dl1, dl2, dl3).getDimensions();

    assertUnorderedEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void mergeMultipleDimensionListsOverwriting() {
    DimensionList dl1 =
        DimensionList.create(
            Dimension.create("dimension1", "default1"),
            Dimension.create("dimension2", "default2"),
            Dimension.create("dimension3", "default3"));
    DimensionList dl2 =
        DimensionList.create(
            Dimension.create("dimension1", "label1"), Dimension.create("dimension2", "label2"));
    DimensionList dl3 = DimensionList.create(Dimension.create("dimension1", "overwriting1"));

    Collection<Dimension> expected =
        Arrays.asList(
            Dimension.create("dimension1", "overwriting1"),
            Dimension.create("dimension2", "label2"),
            Dimension.create("dimension3", "default3"));
    Collection<Dimension> actual = DimensionList.merge(dl1, dl2, dl3).getDimensions();

    assertUnorderedEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void mergeSingleDimensionListNoCollision() {
    DimensionList dl1 =
        DimensionList.create(
            Dimension.create("dimension1", "default1"),
            Dimension.create("dimension2", "default2"),
            Dimension.create("dimension3", "default3"));

    Collection<Dimension> expected =
        Arrays.asList(
            Dimension.create("dimension1", "default1"),
            Dimension.create("dimension2", "default2"),
            Dimension.create("dimension3", "default3"));
    Collection<Dimension> actual = DimensionList.merge(dl1).getDimensions();

    assertUnorderedEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void mergeSingleDimensionListOverwriting() {
    DimensionList dl1 =
        DimensionList.create(
            Dimension.create("dimension", "default1"),
            Dimension.create("dimension", "default2"),
            Dimension.create("dimension", "default3"));

    Collection<Dimension> expected =
        Collections.singletonList(Dimension.create("dimension", "default3"));
    Collection<Dimension> actual = DimensionList.merge(dl1).getDimensions();

    assertUnorderedEquals(expected, actual);
    assertNotSame(expected, actual);
  }

  @Test
  void serializeEmpty() {
    DimensionList dl = DimensionList.create();
    String expected = "";
    String actual = dl.serialize();
    assertEquals(expected, actual);
  }

  @Test
  void serializeOneElement() {
    DimensionList dl = DimensionList.create(Dimension.create("key", "value"));
    String expected = "key=value";
    String actual = dl.serialize();
    assertEquals(expected, actual);
  }

  @Test
  void serializeMultipleElements() {
    // the normalization itself does not prohibit duplicated keys and leaves the order intact.
    DimensionList dl =
        DimensionList.create(
            Dimension.create("key1", "value1"),
            Dimension.create("key2", "value2"),
            Dimension.create("key3", "value3"));
    String expected = "key1=value1,key2=value2,key3=value3";
    String actual = dl.serialize();
    assertEquals(expected, actual);
  }

  @Test
  void serializeMultipleElementsWithDroppedInvalid() {
    DimensionList dl =
        DimensionList.create(
            Dimension.create("key1", "value1"),
            Dimension.create("~@#$", "value2"),
            Dimension.create("key3", "value3"));
    String expected = "key1=value1,_=value2,key3=value3";
    String actual = dl.serialize();
    assertEquals(expected, actual);
  }
}
