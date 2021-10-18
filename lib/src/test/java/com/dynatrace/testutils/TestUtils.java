package com.dynatrace.testutils;

import java.io.File;
import java.util.Random;
import java.util.UUID;

public class TestUtils {
  public static String generateNonExistentFilename() {
    File f;
    Random r = new Random();
    // generate random filenames until we find one that does not exist:
    do {
      String filename =
          "src/test/resources/" + UUID.randomUUID() + ".properties";

      f = new File(filename);
    } while (f.exists());
    return f.getAbsolutePath();
  }
}
