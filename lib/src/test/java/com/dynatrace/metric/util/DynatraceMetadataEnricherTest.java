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

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DynatraceMetadataEnricherTest {
  private static final String METADATA_FILE = "src/test/resources/metadata_file.properties";

  @TempDir Path tempDir;

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

    Properties results =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            "src/test/resources/indirection.properties", nonExistentAlternativeFilename);
    assertEquals(expectedMetadata(), results);
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

    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(filename, METADATA_FILE);

    assertEquals(expectedMetadata(), result);
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileEmptyAndAlternativeDoesNotExist()
      throws IOException {
    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            writeFile("indirection.properties", ""), generateNonExistentFilename());
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionFileEmptyAndAlternativeExists()
      throws IOException {
    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            writeFile("indirection.properties", ""), METADATA_FILE);
    assertEquals(expectedMetadata(), result);
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionPathIsDirectoryAndAlternativeDoesNotExist() {
    // a directory cannot be opened as a file, so this behaves like a missing indirection file
    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            tempDir.toString(), generateNonExistentFilename());
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetPropertiesWithIndirection_IndirectionPathIsDirectoryAndAlternativeExists() {
    // a directory cannot be opened as a file, so this behaves like a missing indirection file
    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(tempDir.toString(), METADATA_FILE);
    assertEquals(expectedMetadata(), result);
  }

  @Test
  void testGetPropertiesWithIndirection_MetadataFileDoesNotExist() throws IOException {
    // call with an existing metadata file as alternative.
    // if the test failed, the alternative file would be read and results would be returned
    // which in turn would break the test.
    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            writeFile("indirection.properties", generateNonExistentFilename()), METADATA_FILE);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetPropertiesWithIndirection_EmptyMetadataFile() throws IOException {
    String metadataFile = writeFile("metadata.properties", "");
    Properties result =
        DynatraceMetadataEnricher.getPropertiesWithIndirection(
            writeFile("indirection.properties", metadataFile), generateNonExistentFilename());
    assertTrue(result.isEmpty());
  }

  @Test
  void testFileExistsAndIsReadable() {
    assertFalse(DynatraceMetadataEnricher.fileExistsAndIsReadable(null));
    assertFalse(DynatraceMetadataEnricher.fileExistsAndIsReadable(""));
    assertFalse(DynatraceMetadataEnricher.fileExistsAndIsReadable(generateNonExistentFilename()));

    assertTrue(DynatraceMetadataEnricher.fileExistsAndIsReadable(METADATA_FILE));
  }

  private String writeFile(String name, String content) throws IOException {
    Path file = tempDir.resolve(name);
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    return file.toString();
  }

  private static Properties expectedMetadata() {
    Properties expected = new Properties();
    expected.setProperty("key1", "value1");
    expected.setProperty("key2", "value2");
    expected.setProperty("key3", "value3");
    return expected;
  }

  private static Stream<Arguments> provideInvalidPropertyParameters() {
    return Stream.of(
        Arguments.of("key_no_value", ""), Arguments.of("", "value_no_key"), Arguments.of("", ""));
  }
}
