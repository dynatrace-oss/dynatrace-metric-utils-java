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
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class DynatraceMetadataEnricher {
  private static final Logger logger = Logger.getLogger(DynatraceMetadataEnricher.class.getName());

  private static final String INDIRECTION_FILE_NAME =
    "dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties";

  private static final String ALTERNATIVE_METADATA_FILENAME =
    "/var/lib/dynatrace/enrichment/dt_metadata.properties";

  /**
   * Retrieve Dynatrace metadata. Attempts to read from the indirection file, and falls back to the
   * alternative metadata file if the primary source is not available.
   *
   * @return A list of not yet normalized {@link Dimension} objects. Items with no equal sign, or
   * with empty key or value are discarded.
   */
  static List<Dimension> getDynatraceMetadata() {
    return createDimensionList(
      getPropertiesWithIndirection(
        INDIRECTION_FILE_NAME, ALTERNATIVE_METADATA_FILENAME));
  }

  /**
   * This function takes {@link Properties} object and transforms it into a {@link List<Dimension>}.
   *
   * @param properties {@link Properties} to transform
   * @return A {@link List} of {@link Dimension dimensions} mapping {@link String} to {@link
   * String}. These represent the property entries, where empty keys or values were omitted.
   */
  static List<Dimension> createDimensionList(Properties properties) {
    ArrayList<Dimension> entries = new ArrayList<>();

    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String value = entry.getValue().toString();

      // make sure key and value are set to non-empty values
      if (key.isEmpty() || value.isEmpty()){
        logger.log(Level.WARNING, () -> String.format("dropped settings %s=%s", key, value));
        continue;
      }

      entries.add(Dimension.create(entry.getKey().toString(), entry.getValue().toString()));
    }

    return entries;
  }

  /**
   * Get the file name of the file in which the metadata is stored.
   *
   * @param fileContents A {@link Reader} object containing the contents of the indirection file.
   * @return The string containing the filename or null if the file is empty.
   * @throws IOException if an error occurs during reading of the file.
   */
  static String getMetadataFileName(Reader fileContents) throws IOException {
    if (fileContents == null) {
      throw new IOException("passed Reader cannot be null.");
    }

    String metadataFileName = null;

    try (BufferedReader reader = new BufferedReader(fileContents)) {
      String line = reader.readLine();
      if (line != null && !line.isEmpty()) {
        metadataFileName = line;
      }
    }
    return metadataFileName;
  }

  /**
   * A helper function that returns whether file exists and is readable.
   *
   * @param filePath The path to the file.
   * @return true if the file exists and is readable and false otherwise.
   */
  static boolean fileExistsAndIsReadable(String filePath) {
    try {
      File file = new File(filePath);
      if (file.exists() && Files.isReadable(file.toPath())) {
        return true;
      }
    } catch (Exception ignored) {
      // something went wrong, but at this point we don't care what it was.
    }
    return false;
  }

  /**
   * Gets the {@link Properties} contained in the metadata file from the indirection file
   * If the indirection file does not exist, falls back to the alternative metadata file.
   *
   * @return The {@link Properties} contained in the Dynatrace metadata file.
   */
  static Properties getPropertiesWithIndirection(
    String indirectionFileName, String alternativeMetadataFilename) {
    String metadataFileName = null;
    Properties props = new Properties();

    try (Reader indirectionFileReader = new FileReader(indirectionFileName)) {
      metadataFileName = getMetadataFileName(indirectionFileReader);
    } catch (FileNotFoundException e) {
      logger.info("Indirection file not found. This is normal if OneAgent is not installed.");
    } catch (Exception e) {
      logger.info(
        String.format("Error while trying to read contents of OneAgent indirection file: %s", e));
    }

    if (metadataFileName == null || metadataFileName.isEmpty()) {
      if (DynatraceMetadataEnricher.fileExistsAndIsReadable(alternativeMetadataFilename)) {
        // alternative file exists, use it for metadata enrichment
        logger.log(Level.INFO, () -> String.format(
          "Alternative metadata file exists, attempting to read from %s.",
          alternativeMetadataFilename));
        metadataFileName = alternativeMetadataFilename;
      } else {
        // no alternative file exists, return empty properties.
        return props;
      }
    }

    try (Reader reader = new FileReader(metadataFileName)) {
      props.load(reader);
    } catch (FileNotFoundException e) {
      logger.warning("Failed to read properties file: File not found");
    } catch (Exception e) {
      logger.info(
        String.format("Error while trying to read contents of Dynatrace metadata file: %s", e));
    }

    return props;
  }
}
