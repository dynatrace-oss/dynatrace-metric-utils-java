package com.dynatrace.metric.util;

final class CodePoints {
  static final int QUOTE = "\"".codePointAt(0);
  static final int BACKSLASH = "\\".codePointAt(0);
  static final int FORWARDSLASH = "/".codePointAt(0);
  static final int COMMA = ",".codePointAt(0);
  static final int EQUALS = "=".codePointAt(0);
  static final int BLANK = " ".codePointAt(0);
  static final int A_LOWERCASE = "a".codePointAt(0);
  static final int Z_LOWERCASE = "z".codePointAt(0);
  static final int A_UPPERCASE = "A".codePointAt(0);
  static final int Z_UPPERCASE = "Z".codePointAt(0);
  static final int UNDERSCORE = "_".codePointAt(0);
  static final int NUMBER_SIGN = "#".codePointAt(0);
  static final int PERCENT_SIGN = "%".codePointAt(0);
  static final int OPEN_SQUARE_BRACKET = "[".codePointAt(0);
  static final int CLOSE_SQUARE_BRACKET = "]".codePointAt(0);
  static final int ZERO = "0".codePointAt(0);
  static final int NINE = "9".codePointAt(0);
  static final int UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_START = 0x1F900;
  static final int UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_END = 0x1F9FF;
  static final String QUOTATION_MARK = "\"";

  private CodePoints() {}
}
