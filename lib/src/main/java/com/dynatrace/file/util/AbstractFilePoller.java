package com.dynatrace.file.util;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class AbstractFilePoller implements Closeable {
  protected final Path absoluteFilePath;

  protected AbstractFilePoller(Path filename) {
    if (filename == null) {
      throw new IllegalArgumentException("filename cannot be null.");
    }

    Path absPath = filename.toAbsolutePath();
    if (Files.isDirectory(absPath)) {
      throw new IllegalArgumentException(String.format("Passed path is a directory: %s", absPath));
    }

    if (!Files.exists(absPath)) {
      throw new IllegalArgumentException(String.format("File does not exist: %s", absPath));
    }

    this.absoluteFilePath = absPath;
  }

  public abstract boolean fileContentsChanged();

  public String getWatchedFilePath() {
    return absoluteFilePath.toString();
  }
}
