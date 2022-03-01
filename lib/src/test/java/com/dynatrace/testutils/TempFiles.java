package com.dynatrace.testutils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFiles implements Closeable {
  private final Path directory;
  private final Path tempFile1;
  private final Path tempFile2;

  public Path getDirectory() {
    return directory;
  }

  public String tempFile1Name() {
    return tempFile1.toString();
  }

  public String tempFile2Name() {
    return tempFile2.toString();
  }

  public Path tempFile1() {
    return tempFile1;
  }

  public Path tempFile2() {
    return tempFile2;
  }

  public TempFiles() throws IOException {
    directory = Files.createTempDirectory("tempdir");
    tempFile1 = Files.createTempFile(directory, "tempfile1_", ".tmp");
    tempFile2 = Files.createTempFile(directory, "tempfile2_", ".tmp");
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(tempFile1);
    Files.deleteIfExists(tempFile2);
    Files.deleteIfExists(directory);
  }
}
