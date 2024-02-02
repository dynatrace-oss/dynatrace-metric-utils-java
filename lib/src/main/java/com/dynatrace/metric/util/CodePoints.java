/**
 * Copyright 2023 Dynatrace LLC
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

final class CodePoints {
  static final int QUOTE = "\"".codePointAt(0);
  static final int BACKSLASH = "\\".codePointAt(0);
  static final int COMMA = ",".codePointAt(0);
  static final int EQUALS = "=".codePointAt(0);
  static final int BLANK = " ".codePointAt(0);
  static final int COLON = ":".codePointAt(0);

  static final int ZERO = "0".codePointAt(0);
  static final int NINE = "9".codePointAt(0);

  static final int A_LOWERCASE = "a".codePointAt(0);
  static final int Z_LOWERCASE = "z".codePointAt(0);
  static final int A_UPPERCASE = "A".codePointAt(0);
  static final int Z_UPPERCASE = "Z".codePointAt(0);

  static final int DOT = ".".codePointAt(0);
  static final String DOT_AS_STRING = new String(Character.toChars(DOT));
  static final int HYPHEN = "-".codePointAt(0);
  static final int UNDERSCORE = "_".codePointAt(0);
  static final int NUMBER_SIGN = "#".codePointAt(0);

  static final int PERCENT_SIGN = "%".codePointAt(0);
  static final int OPEN_SQUARE_BRACKET = "[".codePointAt(0);
  static final int CLOSED_SQUARE_BRACKET = "]".codePointAt(0);
  static final int OPEN_CURLY_BRACKET = "{".codePointAt(0);
  static final int CLOSED_CURLY_BRACKET = "}".codePointAt(0);
  static final int FORWARD_SLASH = "/".codePointAt(0);

  static final int UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_START = 0x1F900;
  static final int UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_END = 0x1F9FF;

  static final String QUOTATION_MARK = "\"";
  static final String EMPTY_STRING = "";

  private CodePoints() {}
}
