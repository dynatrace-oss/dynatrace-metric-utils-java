package com.dynatrace.metric.util.validation;

public final class CodePoints {

  public static final int QUOTE = "\"".codePointAt(0);
  public static final int BACKSLASH = "\\".codePointAt(0);
  public static final int COMMA = ",".codePointAt(0);
  public static final int EQUALS = "=".codePointAt(0);
  public static final int BLANK = " ".codePointAt(0);
  public static final int A_LOWERCASE = "a".codePointAt(0);
  public static final int Z_LOWERCASE = "z".codePointAt(0);
  public static final int A_UPPERCASE = "A".codePointAt(0);
  public static final int Z_UPPERCASE = "Z".codePointAt(0);
  public static final int UNDERSCORE = "_".codePointAt(0);
  public static final int NUMBER_SIGN = "#".codePointAt(0);
  public static final int UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_START = 0x1F900;
  public static final int UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_END = 0x1F9FF;
  public static final String QUOTATION_MARK = "\"";

  private CodePoints() {
  }
}
