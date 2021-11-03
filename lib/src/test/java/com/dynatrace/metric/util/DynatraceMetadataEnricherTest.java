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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DynatraceMetadataEnricherTest {

  @Test
  public void validMetrics() {
    ArrayList<Dimension> entries =
        new ArrayList<>(
            DynatraceMetadataEnricher.parseDynatraceMetadata(
                Arrays.asList("prop.a=value.a", "prop.b=value.b")));

    assertEquals("prop.a", entries.get(0).getKey());
    assertEquals("value.a", entries.get(0).getValue());
    assertEquals("prop.b", entries.get(1).getKey());
    assertEquals("value.b", entries.get(1).getValue());
  }

  @Test
  public void invalidMetrics() {
    assertTrue(
        DynatraceMetadataEnricher.parseDynatraceMetadata(Collections.singletonList("key_no_value="))
            .isEmpty());
    assertTrue(
        DynatraceMetadataEnricher.parseDynatraceMetadata(Collections.singletonList("=value_no_key"))
            .isEmpty());
    assertTrue(
        DynatraceMetadataEnricher.parseDynatraceMetadata(
                Collections.singletonList("==============="))
            .isEmpty());
    assertTrue(
        DynatraceMetadataEnricher.parseDynatraceMetadata(Collections.singletonList("")).isEmpty());
    assertTrue(
        DynatraceMetadataEnricher.parseDynatraceMetadata(Collections.singletonList("=")).isEmpty());
    assertTrue(DynatraceMetadataEnricher.parseDynatraceMetadata(Collections.emptyList()).isEmpty());
  }

  @Test
  public void testGetIndirectionFileContentValid() throws IOException {
    String expected =
        "dt_metadata_e617c525669e072eebe3d0f08212e8f2_private_target_file_specifier.properties";
    // "mock" the contents of dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties
    StringReader reader = new StringReader(expected);
    String result = DynatraceMetadataEnricher.getMetadataFileName(reader);
    assertEquals(expected, result);
  }

  @Test
  public void testGetIndirectionFilePassNull() {
    assertThrows(IOException.class, () -> DynatraceMetadataEnricher.getMetadataFileName(null));
  }

  @Test
  public void testGetIndirectionFileContentEmptyContent() throws IOException {
    StringReader reader = new StringReader("");
    assertNull(DynatraceMetadataEnricher.getMetadataFileName(reader));
  }

  @Test
  public void testGetDynatraceMetadataFileContentValid() throws IOException {
    List<String> expected = new ArrayList<>();
    expected.add("key1=value1");
    expected.add("key2=value2");
    expected.add("key3=value3");

    StringReader reader = new StringReader(String.join("\n", expected));
    List<String> result = DynatraceMetadataEnricher.getDynatraceMetadataFileContents(reader);
    assertEquals(expected, result);
    assertNotSame(expected, result);
  }

  @Test
  public void testGetDynatraceMetadataFileContentInvalid() throws IOException {
    List<String> inputs = Arrays.asList("=0", "", "a=", "\t\t", "=====", "    ", "   test   ");
    List<String> expected = Arrays.asList("=0", "", "a=", "", "=====", "", "test");

    StringReader reader = new StringReader(String.join("\n", inputs));
    List<String> result = DynatraceMetadataEnricher.getDynatraceMetadataFileContents(reader);
    assertEquals(expected, result);
    assertNotSame(expected, result);
  }

  @Test
  public void testGetDynatraceMetadataFileContentEmptyFile() throws IOException {
    List<String> expected = new ArrayList<>();

    List<String> result =
        DynatraceMetadataEnricher.getDynatraceMetadataFileContents(new StringReader(""));
    assertEquals(expected, result);
    assertNotSame(expected, result);
  }

  @Test
  public void testGetDynatraceMetadataFileContentPassNull() {
    assertThrows(
        IOException.class, () -> DynatraceMetadataEnricher.getDynatraceMetadataFileContents(null));
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_Valid() {
    // this should not be used. If it is, it will not exist and throw an exception, breaking the
    // test.
    String nonExistentAlternativeFilename = generateNonExistentFilename();
    List<String> expected = Arrays.asList("key1=value1", "key2=value2", "key3=value3");
    List<String> results =
        DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
            "src/test/resources/indirection.properties", nonExistentAlternativeFilename);
    assertEquals(expected, results);
    assertNotSame(expected, results);
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileDoesNotExistAlternativeDoesNotExist() {
    String filename = generateNonExistentFilename();
    String alternativeFilename = generateNonExistentFilename();

    List<String> result =
        DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
            filename, alternativeFilename);
    assertEquals(Collections.<String>emptyList(), result);
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileDoesNotExistAlternativeExists() {
    String filename = generateNonExistentFilename();

    List<String> result =
        DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
            filename, "src/test/resources/metadata_file.properties");
    List<String> expected = Arrays.asList("key1=value1", "key2=value2", "key3=value3");
    assertEquals(expected, result);
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileReturnsNullAndAlternativeDoesNotExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties", nonExistentAlternativeFile);
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileReturnsNullAndAlternativeDoesExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getDynatraceMetadataFileContents(
                      Mockito.any(FileReader.class)))
          .thenCallRealMethod();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");
      List<String> expected = Arrays.asList("key1=value1", "key2=value2", "key3=value3");
      assertEquals(expected, result);
    }
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileReturnsEmptyAndAlternativeDoesNotExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties", nonExistentAlternativeFile);
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileReturnsEmptyAndAlternativeDoesExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getDynatraceMetadataFileContents(
                      Mockito.any(FileReader.class)))
          .thenCallRealMethod();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");
      List<String> expected = Arrays.asList("key1=value1", "key2=value2", "key3=value3");
      assertEquals(expected, result);
    }
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileThrowsAndAlternativeDoesNotExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties", nonExistentAlternativeFile);
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void
      testGetMetadataFileContentWithRedirection_IndirectionFileThrowsAndAlternativeDoesExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getDynatraceMetadataFileContents(
                      Mockito.any(FileReader.class)))
          .thenCallRealMethod();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");
      List<String> expected = Arrays.asList("key1=value1", "key2=value2", "key3=value3");
      assertEquals(expected, result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_MetadataFileDoesNotExist() {
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
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getDynatraceMetadataFileContents(
                      Mockito.any(FileReader.class)))
          .thenCallRealMethod();

      // call with an existing indirection target as alternative.
      // if the test failed, the alternative file would be read and results would be returned
      // which in turn would break the test.
      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties",
              "src/test/resources/metadata_file.properties");

      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_MetadataFileReadThrows() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("src/test/resources/mock_target.properties");
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getDynatraceMetadataFileContents(
                      Mockito.any(FileReader.class)))
          .thenThrow(new IOException("test exception"));
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      // this should never be used but is required.
      String alternativeFileName = generateNonExistentFilename();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties", alternativeFileName);
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_EmptyMetadataFile() {
    try (MockedStatic<DynatraceMetadataEnricher> mockEnricher =
        Mockito.mockStatic(DynatraceMetadataEnricher.class)) {
      mockEnricher
          .when(() -> DynatraceMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("src/test/resources/mock_target.properties");
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getDynatraceMetadataFileContents(
                      Mockito.any(FileReader.class)))
          .thenReturn(Collections.emptyList());
      mockEnricher
          .when(
              () ->
                  DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString(), Mockito.anyString()))
          .thenCallRealMethod();

      // this should never be used but is required.
      String alternativeFileName = generateNonExistentFilename();

      List<String> result =
          DynatraceMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties", alternativeFileName);
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testFileExistsAndIsReadable() {
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
}
