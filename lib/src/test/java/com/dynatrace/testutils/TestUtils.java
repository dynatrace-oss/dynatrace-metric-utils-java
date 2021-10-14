package com.dynatrace.testutils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TestUtils {
    public static String generateNonExistentFilename() {
        File f;
        Random r = new Random();
        // generate random filenames until we find one that does not exist:
        do {
            byte[] array = new byte[7];
            r.nextBytes(array);
            String filename =
                    "src/test/resources/" + new String(array, StandardCharsets.UTF_8) + ".properties";

            f = new File(filename);
        } while (f.exists());
        return f.getAbsolutePath();
    }
}
