/**
 * Copyright 2022 Dynatrace LLC
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
package com.dynatrace.testutils;

import java.io.File;
import java.util.UUID;

public class TestUtils {
  public static String generateNonExistentFilename() {
    File f;
    // generate random filenames until we find one that does not exist:
    do {
      String filename = "src/test/resources/" + UUID.randomUUID() + ".properties";

      f = new File(filename);
    } while (f.exists());
    return f.getAbsolutePath();
  }

  public static String codePointToString(int codePoint) {
    return new String(Character.toChars(codePoint));
  }

  public static String repeatStringNTimes(String s, int n) {
    return new String(new char[n]).replace("\0", s);
  }

  public static String createStringOfLength(int n, boolean quoted) {
    if (quoted) {
      return "\"" + repeatStringNTimes("a", n) + "\"";
    }
    return repeatStringNTimes("a", n);
  }
}
