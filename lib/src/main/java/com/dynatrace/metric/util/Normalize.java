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
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

class Normalize {
  private static final Logger logger = Logger.getLogger(Normalize.class.getName());

  //      # Metric keys (mk)
  //    # characters not valid to start the first identifier key section
  //  __re_mk_first_identifier_section_start = (re.compile(r"^[^a-zA-Z_]+"))
  //
  //          # characters not valid to start subsequent identifier key sections
  //          __re_mk_identifier_section_start = re.compile(r"^[^a-zA-Z0-9_]+")
  //  __re_mk_identifier_section_end = re.compile(r"[^a-zA-Z0-9_\-]+$")
  //
  //          # for the rest of the metric key characters, alphanumeric characters as
  //    # well as hyphens and underscores are allowed. consecutive invalid
  //    # characters will be condensed into one underscore.
  //          __re_mk_invalid_characters = re.compile(r"[^a-zA-Z0-9_\-]+")
  //
  //  __mk_max_length = 250
  //
  //          # Dimension keys (dk)
  //    # dimension keys have to start with a lowercase letter or an underscore.
  //  __re_dk_start = re.compile(r"^[^a-z_]+")
  //  __re_dk_end = re.compile(r"[^a-z0-9_\-:]+$")
  //
  //          # other valid characters in dimension keys are lowercase letters, numbers,
  //          # colons, underscores and hyphens.
  //          __re_dk_invalid_chars = re.compile(r"[^a-z0-9_\-:]+")
  //
  //  __dk_max_length = 100
  //
  //          # Dimension values (dv)
  //    # all control characters (cc) are replaced with the null character (\u0000)
  //    # and then removed as appropriate using the following regular expressions.
  //  __re_dv_cc = re.compile(r"\u0000+")
  //  __re_dv_cc_leading = re.compile(r"^" + __re_dv_cc.pattern)
  //  __re_dv_cc_trailing = re.compile(__re_dv_cc.pattern + r"$")
  //
  //          # characters to be escaped in the dimension value
  //  __re_dv_escape_chars = re.compile(r"([= ,\\])")
  //
  //  __dv_max_length = 250

  private static final int mk_max_length = 250;
  private static final int dk_max_length = 100;
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
      throw new IllegalArgumentException("key cannot be null or empty");
    }
    if (key.length() > dk_max_length) {
      key = key.substring(0, dk_max_length);
    }
    // todo: normalization
    return key;
  }

  static String dimensionValue(String value) {
    if (value.length() > dv_max_length) {
      value = value.substring(0, dv_max_length);
    }
    // remove control characters.
    value = CharMatcher.javaIsoControl().removeFrom(value);

    // todo
    return value;
  }

  static String metricKey(String key) {
    if (key.length() > mk_max_length) {
      key = key.substring(0, mk_max_length);
    }

    // todo
    return key;
  }
}
