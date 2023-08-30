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

import static com.dynatrace.testutils.TestUtils.generateNonExistentFilename;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DynatraceMetadataEnricherTest {

  @Test
  void validProperties() {
    Properties properties = new Properties();
    properties.setProperty("prop.a", "value.a");
    properties.setProperty("prop.b", "value.b");

    Map<String, String> entries = DynatraceMetadataEnricher.createMapFromProperties(properties);

    // Has one entry with key "prop.a"
    assertTrue(entries.containsKey("prop.a"));

    // Entry with key "prop.a" has value "value.a"
    assertEquals("value.a", entries.get("prop.a"));

    // Has one entry with key "prop.p"
    assertTrue(entries.containsKey("prop.b"));

    // Entry with key "prop.b" has value "value.b"
    assertEquals("value.b", entries.get("prop.b"));
  }

  @ParameterizedTest(
      name = "{index}: createDimensionList with key `{0}` and value `{1}` should drop entry")
  @MethodSource("provideInvalidPropertyParameters")
  void invalidProperties(String key, String value) {
    Properties properties = new Properties();
    properties.setProperty(key, value);
    assertTrue(DynatraceMetadataEnricher.createMapFromProperties(properties).isEmpty());
  }

  @Test
  void testGetIndirectionFileContentValid() throws IOException {
    String expected =
        "dt_metadata_e617c525669e072eebe3d0f08212e8f2_private_target_file_specifier.properties";
    // "mock" the contents of dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties
    StringReader reader = new StringReader(expected);
    String result = DynatraceMetadataEnricher.getMetadataFileName(reader);
    assertEquals(expected, result);
  }

  @Test
  void testGetIndirectionFilePassNull() {
    assertThrows(IOException.class, () -> DynatraceMetadataEnricher.getMetadataFileName(null));
  }

  @Test
  void testGetIndirectionFileContentEmptyContent() throws IOException {
    StringReader reader = new StringReader("");
    assertNull(DynatraceMetadataEnricher.getMetadataFileName(reader));
  }

  @Test
  void testGetPropertiesWithIndirection_Valid() {
    // this should not be used. If it is, it will not exist and throw an exception, breaking the
    // test.
    String nonExistentAlternativeFilename = generateNonExistentFilename();
    Properties expected = new Properties();
    expected.setProperty("key1", "value1");
    expected.setProperty("key2", "value2");
    expected.setProperty("key3", "value3");

    Properties results =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            "src/test/resources/indirection.properties", nonExistentAlternativeFilename);
    assertEquals(expected, results);
    assertNotSame(expected, results);
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileDoesNotExistAlternativeDoesNotExist() {
    String filename = generateNonExistentFilename();
    String alternativeFilename = generateNonExistentFilename();

    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(filename, alternativeFilename);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileDoesNotExistAlternativeExists() {
    String filename = generateNonExistentFilename();

    Properties expected = new Properties();
    expected.setProperty("key1", "value1");
    expected.setProperty("key2", "value2");
    expected.setProperty("key3", "value3");

    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            filename, "src/test/resources/metadata_file.properties");

    assertEquals(expected, result);
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileReturnsNullAndAlternativeDoesNotExist() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn(null);
      String nonExistentAlternativeFile = generateNonExistentFilename();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq(nonExistentAlternativeFile)))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties", nonExistentAlternativeFile);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileReturnsNullAndAlternativeDoesExist() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn(null);
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq("src/test/resources/metadata_file.properties")))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      Properties expected = new Properties();
      expected.setProperty("key1", "value1");
      expected.setProperty("key2", "value2");
      expected.setProperty("key3", "value3");

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");

      assertEquals(expected, result);
    }
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileReturnsEmptyAndAlternativeDoesNotExist() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      // ignore the return value of the testfile and mock the return value of the
      // getIndirectionFileName call:
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("");
      String nonExistentAlternativeFile = generateNonExistentFilename();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq(nonExistentAlternativeFile)))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties", nonExistentAlternativeFile);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileReturnsEmptyAndAlternativeDoesExist() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("");
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq("src/test/resources/metadata_file.properties")))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      Properties expected = new Properties();
      expected.setProperty("key1", "value1");
      expected.setProperty("key2", "value2");
      expected.setProperty("key3", "value3");

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");

      assertEquals(expected, result);
    }
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileThrowsAndAlternativeDoesNotExist() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      // ignore the return value of the testfile and mock the return value of the
      // getIndirectionFileName call:
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenThrow(new IOException("test exception"));
      String nonExistentAlternativeFile = generateNonExistentFilename();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq(nonExistentAlternativeFile)))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties", nonExistentAlternativeFile);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileThrowsAndAlternativeDoesExist() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenThrow(new IOException("test exception"));
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq("src/test/resources/metadata_file.properties")))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      Properties expected = new Properties();
      expected.setProperty("key1", "value1");
      expected.setProperty("key2", "value2");
      expected.setProperty("key3", "value3");

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");

      assertEquals(expected, result);
    }
  }

  @Test
  void testGetPropertiesWithIndirection_MetadataFileDoesNotExist() {
    String metadataFilename = generateNonExistentFilename();
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn(metadataFilename);
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.fileExistsAndIsReadable(
                      Mockito.eq("src/test/resources/metadata_file.properties")))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      // call with an existing indirection target as alternative.
      // if the test failed, the alternative file would be read and results would be returned
      // which in turn would break the test.
      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");

      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testGetPropertiesWithIndirection_EmptyMetadataFile() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("src/test/resources/mock_target.properties");
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getPropertiesWithIndirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      // this should never be used but is required.
      String alternativeFileName = generateNonExistentFilename();

      Properties result =
          DynatraceMetadataEnricher.getPropertiesWithIndirection(
              "src/test/resources/mock_target.properties", alternativeFileName);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testFileExistsAndIsReadable() {
    assertFalse(DynatraceMetadataEnricher.fileExistsAndIsReadable(null));
    assertFalse(DynatraceMetadataEnricher.fileExistsAndIsReadable(""));
    assertFalse(DynatraceMetadataEnricher.fileExistsAndIsReadable(generateNonExistentFilename()));

    assertTrue(
        DynatraceMetadataEnricher.fileExistsAndIsReadable(
            "src/test/resources/mock_target.properties"));
    assertTrue(
        DynatraceMetadataEnricher.fileExistsAndIsReadable(
            "src/test/resources/metadata_file.properties"));
  }

  private static Stream<Arguments> provideInvalidPropertyParameters() {
    return Stream.of(
        Arguments.of("key_no_value", ""), Arguments.of("", "value_no_key"), Arguments.of("", ""));
  }
}
