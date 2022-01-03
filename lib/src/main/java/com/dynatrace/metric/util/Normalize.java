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
  // invalid characters for the rest of the key.
  private static final Pattern re_mk_invalidCharacters = Pattern.compile("[^a-zA-Z0-9_\\-]+");

  // maximum string length of a metric key.
  private static final int mk_max_length = 250;

  // Dimension keys (dk)
  // Dimension keys start with a lowercase letter or an underscore.
  private static final Pattern re_dk_sectionStart = Pattern.compile("^[^a-z_]+");
  // invalid characters in the rest of the dimension key
  private static final Pattern re_dk_invalidCharacters = Pattern.compile("[^a-z0-9_\\-:]+");

  // maximum string length of a dimension key.
  private static final int dk_max_length = 100;

  // Dimension values (dv)
  // Characters that need to be escaped in dimension values
  private static final char dv_characterToEscape_quote = '"';
  private static final char dv_characterToEscape_equals = '=';
  private static final char dv_characterToEscape_blank = ' ';
  private static final char dv_characterToEscape_comma = ',';
  private static final char dv_characterToEscape_backslash = '\\';
  private static final String dv_charactersToEscape =
      new StringBuilder(5)
          .append(dv_characterToEscape_quote)
          .append(dv_characterToEscape_equals)
          .append(dv_characterToEscape_blank)
          .append(dv_characterToEscape_comma)
          .append(dv_characterToEscape_backslash)
          .toString();
  private static final Pattern re_dv_charactersToEscape =
      Pattern.compile("([" + Pattern.quote(dv_charactersToEscape) + "])");
  private static final Pattern re_dv_controlCharacters = Pattern.compile("[\\p{C}]+");

  // This regex checks if there is an odd number of trailing backslashes in the string. It can be
  // read as: {not a slash}{any number of 2-slash pairs}{one slash}{end line}.
  private static final Pattern re_dv_hasOddNumberOfTrailingBackslashes =
      Pattern.compile("[^\\\\](?:\\\\\\\\)*\\\\$");

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
        // replace consecutive leading invalid characters with an underscore.
        normalizedSection = re_dk_sectionStart.matcher(normalizedSection).replaceAll("_");
        // replace consecutive invalid characters within the section with one underscore:
        normalizedSection = re_dk_invalidCharacters.matcher(normalizedSection).replaceAll("_");

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

  static String dimensionValue(String value) {
    if (value == null) {
      return "";
    }
    if (value.length() > dv_max_length) {
      value = value.substring(0, dv_max_length);
    }
    // collapse contained control chars to an underscore
    value = re_dv_controlCharacters.matcher(value).replaceAll("_");

    return value;
  }

  /**
   * Fast check: Does the dimension value need to be escaped at all?
   *
   * @param dimensionValue The dimension value
   * @return True if it contains characters that need to be escaped according to the Dynatrace
   *     metrics ingestion protocol, false otherwise.
   */
  static boolean needToEscapeDimensionValue(String dimensionValue) {
    int len = dimensionValue.length();
    if (len > dv_max_length) {
      return true;
    }

    for (int i = 0; i < len; i++) {
      char c = dimensionValue.charAt(i);
      if (c == dv_characterToEscape_quote
          || c == dv_characterToEscape_equals
          || c == dv_characterToEscape_blank
          || c == dv_characterToEscape_comma
          || c == dv_characterToEscape_backslash) {
        return true;
      }
    }

    return false;
  }

  static String escapeDimensionValue(String val) {
    // Fast pass: Only escape if escaping is actually necessary
    if (!needToEscapeDimensionValue(val)) {
      return val;
    }

    // escape characters matched by regex with backslash. $1 inserts the matched character.
    String escaped = re_dv_charactersToEscape.matcher(val).replaceAll("\\\\$1");
    if (escaped.length() > dv_max_length) {
      escaped = escaped.substring(0, dv_max_length);
      if (re_dv_hasOddNumberOfTrailingBackslashes.matcher(escaped).find()) {
        // string has trailing backslashes. Since every backslash must be escaped, there must be an
        // even number of backslashes, otherwise the substring operation cut an escaped character
        // in half: e.g.: "some_long_string," -> escaped: "some_long_string\," -> cut with substring
        // results in "some_long_string\" since the two slashes were on either side of the char
        // at which the string was cut using substring. If this is the case, trim the last
        // backslash character, resulting in a properly escaped string.
        escaped = escaped.substring(0, dv_max_length - 1);
      }
    }

    return escaped;
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
      // can check if is empty at the very beginning, because if it is not empty it will always be
      // at least one underscore.
      if (section.isEmpty()) {
        if (firstSection) {
          logger.warning(
              String.format(
                  "first metric key section is empty. discarding metric with key %s...", key));
          return null;
        } else {
          // skip empty sections.
          continue;
        }
      }

      String normalizedSection;
      // first key section cannot start with a number while subsequent sections can.
      if (firstSection) {
        normalizedSection = re_mk_firstIdentifierSectionStart.matcher(section).replaceAll("_");
      } else {
        normalizedSection = re_mk_subsequentIdentifierSectionStart.matcher(section).replaceAll("_");
      }

      // replace invalid chars with an underscore
      normalizedSection = re_mk_invalidCharacters.matcher(normalizedSection).replaceAll("_");

      // re-concatenate the split sections separated with dots.
      if (!firstSection) {
        normalizedKeyBuilder.append(".");
      } else {
        firstSection = false;
      }

      normalizedKeyBuilder.append(normalizedSection);
    }

    return normalizedKeyBuilder.toString();
  }
}
