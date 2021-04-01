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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

final class Normalize {
  private static final Logger logger = Logger.getLogger(Normalize.class.getName());

  //  Metric keys (mk)
  //  characters not valid as leading characters in the first identifier key section
  private static final Pattern re_mk_firstIdentifierSectionStart = Pattern.compile("^[^a-zA-Z_]+");
  // characters not valid as leading characters in subsequent subsections.
  private static final Pattern re_mk_subsequentIdentifierSectionStart =
      Pattern.compile("^[^a-zA-Z0-9_]+");
  // chars that are invalid as trailing characters
  private static final Pattern re_mk_identifierSectionEnd = Pattern.compile("[^a-zA-Z0-9_\\-]+$");
  // invalid characters for the rest of the key.
  private static final Pattern re_mk_invalidCharacters = Pattern.compile("[^a-zA-Z0-9_\\-]+");

  // maximum string length of a metric key.
  private static final int mk_max_length = 250;

  // Dimension keys (dk)
  // Dimension keys start with a lowercase letter or an underscore.
  private static final Pattern re_dk_sectionStart = Pattern.compile("^[^a-z_]+");
  // trailing characters not in this character class are trimmed off.
  private static final Pattern re_dk_sectionEnd = Pattern.compile("[^a-z0-9_\\-:]+$");
  // invalid characters in the rest of the dimension key
  private static final Pattern re_dk_invalidCharacters = Pattern.compile("[^a-z0-9_\\-:]+");

  // maximum string length of a dimension key.
  private static final int dk_max_length = 100;

  // Dimension values (dv)
  // Characters that need to be escaped in dimension values
  private static final Pattern re_dv_charactersToEscape = Pattern.compile("([= ,\\\\])");
  private static final Pattern re_dv_controlCharacters = Pattern.compile("[\\p{C}]+");
  private static final Pattern re_dv_controlCharactersStart = Pattern.compile("^[\\p{C}]+");
  private static final Pattern re_dv_controlCharactersEnd = Pattern.compile("[\\p{C}]+$");

  // maximum string length of a dimension value.
  private static final int dv_max_length = 250;

  private Normalize() {} // static helper class

  private static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }

  /**
   * Normalizes all dimension keys and values for a given collection of Dimensions. Does *not*
   * deduplicate dimensions with the same (normalized) key.
   *
   * @param dimensions The dimensions to normalize.
   * @return A list holding all elements that were not discarded due to invalid keys after
   *     normalization or an empty list if no keys are valid.
   */
  static List<Dimension> dimensionList(Collection<Dimension> dimensions) {
    List<Dimension> normalized = new ArrayList<>();
    if (dimensions == null) {
      return normalized;
    }
    for (Dimension dimension : dimensions) {
      String normalizedKey = dimensionKey(dimension.getKey());
      if (isNullOrEmpty(normalizedKey)) {
        logger.warning(
            String.format(
                "could not normalize dimension key: '%s'. Skipping...", dimension.getKey()));
      } else {
        normalized.add(Dimension.create(normalizedKey, dimensionValue(dimension.getValue())));
      }
    }
    return normalized;
  }

  static String dimensionKey(String key) {
    if (isNullOrEmpty(key)) {
      return "";
    }
    if (key.length() > dk_max_length) {
      key = key.substring(0, dk_max_length);
    }

    String[] sections = key.split("\\.");
    StringBuilder normalizedKeyBuilder = new StringBuilder();
    boolean firstSection = true;

    for (String section : sections) {
      if (!section.isEmpty()) {
        // move to lowercase
        String normalizedSection = section.toLowerCase(Locale.ROOT);
        // trim leading and trailing invalid characters.
        normalizedSection = re_dk_sectionStart.matcher(normalizedSection).replaceAll("");
        normalizedSection = re_dk_sectionEnd.matcher(normalizedSection).replaceAll("");
        // replace consecutive invalid characters within the section with one underscore:
        normalizedSection = re_dk_invalidCharacters.matcher(normalizedSection).replaceAll("_");

        if (normalizedSection.isEmpty()) {
          // section is empty after normalization and will be discarded.
          logger.info(
              String.format(
                  "normalization of section '%s' lead to empty section, discarding...", section));
        } else {
          // re-concatenate the split sections separated with dots.
          if (!firstSection) {
            normalizedKeyBuilder.append(".");
          } else {
            firstSection = false;
          }

          normalizedKeyBuilder.append(normalizedSection);
        }
      }
    }
    return normalizedKeyBuilder.toString();
  }

  static String dimensionValue(String value) {
    if (value == null) {
      return "";
    }
    if (value.length() > dv_max_length) {
      value = value.substring(0, dv_max_length);
    }
    // trim leading and trailing control characters and collapse contained control chars to an "_"
    value = re_dv_controlCharactersStart.matcher(value).replaceAll("");
    value = re_dv_controlCharactersEnd.matcher(value).replaceAll("");
    value = re_dv_controlCharacters.matcher(value).replaceAll("_");

    // escape characters matched by regex with backslash. $1 inserts the matched character.
    value = re_dv_charactersToEscape.matcher(value).replaceAll("\\\\$1");

    return value;
  }

  static String metricKey(String key) {
    if (isNullOrEmpty(key)) {
      logger.warning("null or empty metric key passed to normalization.");
      return null;
    }

    if (key.length() > mk_max_length) {
      key = key.substring(0, mk_max_length);
    }

    String[] sections = key.split("\\.");
    if (sections.length == 0) {
      return null;
    }
    boolean firstSection = true;
    StringBuilder normalizedKeyBuilder = new StringBuilder();

    for (String section : sections) {
      String normalizedSection;
      // first key section cannot start with a number while subsequent sections can.
      if (firstSection) {
        normalizedSection = re_mk_firstIdentifierSectionStart.matcher(section).replaceAll("");
      } else {
        normalizedSection = re_mk_subsequentIdentifierSectionStart.matcher(section).replaceAll("");
      }

      // trim trailing invalid chars
      normalizedSection = re_mk_identifierSectionEnd.matcher(normalizedSection).replaceAll("");

      // replace invalid chars with an underscore
      normalizedSection = re_mk_invalidCharacters.matcher(normalizedSection).replaceAll("_");

      if (normalizedSection.isEmpty()) {
        if (firstSection) {
          logger.warning(
              String.format(
                  "first metric key section empty while normalizing '%s', discarding...", key));
          return null;
        }
        // section is empty after normalization and will be discarded.
        logger.info(
            String.format(
                "normalization of section '%s' in '%s' leads to empty section, discarding section...",
                section, key));
      } else {
        // re-concatenate the split sections separated with dots.
        if (!firstSection) {
          normalizedKeyBuilder.append(".");
        } else {
          firstSection = false;
        }

        normalizedKeyBuilder.append(normalizedSection);
      }
    }

    return normalizedKeyBuilder.toString();
  }
}
