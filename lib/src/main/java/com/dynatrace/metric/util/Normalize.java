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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

class Normalize {
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
  private static final CharMatcher dv_controlCharsMatcher = CharMatcher.javaIsoControl();

  // maximum string length of a dimension value.
  private static final int dv_max_length = 250;

  static List<Dimension> dimensionList(Collection<Dimension> dimensions) {
    List<Dimension> normalized = new ArrayList<>();
    for (Dimension dimension : dimensions) {
      try {
        String key = dimensionKey(dimension.Key);
        normalized.add(Dimension.create(key, dimensionValue(dimension.Value)));
      } catch (IllegalArgumentException iae) {
        logger.warning(String.format("could not normalize dimension key: '%s'", dimension.Key));
      }
    }
    return normalized;
  }

  static String dimensionKey(String key) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(key)) {
      return "";
    }
    if (key.length() > dk_max_length) {
      key = key.substring(0, dk_max_length);
    }

    Iterable<String> sections = Splitter.on('.').split(key);
    StringBuilder normalizedKeyBuilder = new StringBuilder();
    boolean firstSection = true;

    for (String section : sections) {
      if (Strings.isNullOrEmpty(section)) {
        continue;
      }
      // move to lowercase
      String normalizedSection = section.toLowerCase(Locale.ROOT);
      // trim leading and trailing invalid characters.
      normalizedSection = re_dk_sectionStart.matcher(normalizedSection).replaceAll("");
      normalizedSection = re_dk_sectionEnd.matcher(normalizedSection).replaceAll("");
      // replace consecutive invalid characters within the section with one underscore:
      normalizedSection = re_dk_invalidCharacters.matcher(normalizedSection).replaceAll("_");

      if (Strings.isNullOrEmpty(normalizedSection)) {
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
    return normalizedKeyBuilder.toString();
  }

  static String dimensionValue(String value) {
    if (value.length() > dv_max_length) {
      value = value.substring(0, dv_max_length);
    }
    // trim leading and trailing control characters and collapse contained control chars to an "_"
    value = dv_controlCharsMatcher.trimAndCollapseFrom(value, '_');

    // escape characters matched by regex with backslash. $1 inserts the matched character.
    value = re_dv_charactersToEscape.matcher(value).replaceAll("\\\\$1");

    return value;
  }

  static String metricKey(String key) {
    if (Strings.isNullOrEmpty(key)) {
      return "";
    }
    if (key.length() > mk_max_length) {
      key = key.substring(0, mk_max_length);
    }

    Iterable<String> sections = Splitter.on(".").split(key);
    boolean firstSection = true;
    StringBuilder normalizedKeyBuilder = new StringBuilder();

    for (String section : sections) {
      if (Strings.isNullOrEmpty(section)) {
        if (firstSection) {
          logger.warning("first key section cannot be empty");
          return "";
        }
        continue;
      }

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

      if (Strings.isNullOrEmpty(normalizedSection)) {
        if (firstSection) {
          logger.warning(
              String.format("first key section empty after normalization, was %s", section));
          return "";
        }
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

    return normalizedKeyBuilder.toString();
  }
}
