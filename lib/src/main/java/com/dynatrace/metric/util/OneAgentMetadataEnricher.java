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

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** @deprecated Use {@link DynatraceMetadataEnricher} instead. */
@Deprecated
final class OneAgentMetadataEnricher {
  private static final Logger logger = Logger.getLogger(OneAgentMetadataEnricher.class.getName());
  private static final String INDIRECTION_FILE_NAME =
      "dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties";

  /** @deprecated Use {@link DynatraceMetadataEnricher#getDynatraceMetadata()} instead. */
  @Deprecated
  public static List<Dimension> getDimensionsFromOneAgentMetadata() {
    return parseOneAgentMetadata(getMetadataFileContentWithRedirection(INDIRECTION_FILE_NAME));
  }

  /**
   * This function takes a list of strings from the OneAgent metadata file and transforms it into a
   * list of {@link Dimension} objects. Parsing failures will not be added to the output list,
   * therefore, it is possible that the output list is shorter than the input, or even empty.
   *
   * @param lines a {@link List<String>} containing key-value pairs (as one string) separated by an
   *     equal sign.
   * @return A {@link List} of {@link Dimension dimensions} mapping {@link String} to {@link
   *     String}. These represent the the lines passed in separated by the first occurring equal
   *     sign on each line, respectively. If no line is parsable, returns an empty list.
   */
  static List<Dimension> parseOneAgentMetadata(Collection<String> lines) {
    ArrayList<Dimension> entries = new ArrayList<>();

    // iterate all lines from OneAgent metadata file.
    for (String line : lines) {
      logger.info(String.format("parsing OneAgent metadata: %s", line));
      // if there are more than one '=' in the line, split only at the first one.
      String[] split = line.split("=", 2);

      // occurs if there is no '=' in the line
      if (split.length != 2) {
        logger.warning(String.format("could not parse OneAgent metadata line ('%s')", line));
        continue;
      }

      String key = split[0];
      String value = split[1];

      // make sure key and value are set to non-null, non-empty values
      if ((key == null || key.isEmpty()) || (value == null || value.isEmpty())) {
        logger.warning(String.format("could not parse OneAgent metadata line ('%s')", line));
        continue;
      }
      entries.add(Dimension.create(key, value));
    }
    return entries;
  }

  /**
   * Get the file name of the file in which OneAgent provides the metadata.
   *
   * @param fileContents A {@link Reader} object pointing at the indirection file.
   * @return The string containing the filename or null if the file is empty.
   * @throws IOException if an error occurs during reading of the file.
   */
  static String getMetadataFileName(Reader fileContents) throws IOException {
    if (fileContents == null) {
      throw new IOException("passed Reader cannot be null.");
    }

    String oneAgentMetadataFileName = null;

    try (BufferedReader reader = new BufferedReader(fileContents)) {
      String line = reader.readLine();
      if (line != null && !line.isEmpty()) {
        oneAgentMetadataFileName = line;
      }
    }
    return oneAgentMetadataFileName;
  }

  /**
   * Read the actual content of the OneAgent metadata file.
   *
   * @param fileContents A {@link Reader} object pointing at the metadata file.
   * @return A {@link List<String>} containing the {@link String#trim() trimmed} lines.
   * @throws IOException if an error occurs during reading of the file.
   */
  static List<String> getOneAgentMetadataFileContent(Reader fileContents) throws IOException {
    if (fileContents == null) {
      throw new IOException("passed Reader cannot be null.");
    }
    try (BufferedReader reader = new BufferedReader(fileContents)) {
      return reader.lines().map(String::trim).collect(Collectors.toList());
    }
  }

  /**
   * Gets the file location of the OneAgent metadata file from the indirection file and reads the
   * contents of the OneAgent metadata file.
   *
   * @return A {@link List<String>} representing the contents of the OneAgent metadata file. Leading
   *     and trailing whitespaces are {@link String#trim() trimmed} for each of the lines.
   */
  static List<String> getMetadataFileContentWithRedirection(String indirectionFileName) {
    String oneAgentMetadataFileName = null;

    try (Reader indirectionFileReader = new FileReader(indirectionFileName)) {
      oneAgentMetadataFileName = getMetadataFileName(indirectionFileReader);
    } catch (FileNotFoundException e) {
      logger.info(
          "OneAgent indirection file not found. This is normal if OneAgent is not installed.");
    } catch (IOException e) {
      logger.info(
          String.format(
              "Error while trying to read contents of OneAgent indirection file: %s",
              e.getMessage()));
    }

    if (oneAgentMetadataFileName == null || oneAgentMetadataFileName.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> properties = Collections.emptyList();
    try (Reader metadataFileReader = new FileReader(oneAgentMetadataFileName)) {
      properties = getOneAgentMetadataFileContent(metadataFileReader);
    } catch (FileNotFoundException e) {
      logger.warning("OneAgent indirection file pointed to non existent properties file.");
    } catch (IOException e) {
      logger.info(
          String.format(
              "Error while trying to read contents of OneAgent metadata file: %s", e.getMessage()));
    }
    return properties;
  }
}
