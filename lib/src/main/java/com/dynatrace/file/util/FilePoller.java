package com.dynatrace.file.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Logger;

class FilePoller implements Closeable {
  private final AbstractFilePoller filePoller;
  private static final Logger logger = Logger.getLogger(FilePoller.class.getName());

  private static AbstractFilePoller createFilePoller(
      String fileName, FilePollerKind kind, Duration pollInterval) throws IOException {
    if (kind == FilePollerKind.POLL_BASED) {
      logger.info(
          () ->
              String.format(
                  "Setting up poll-based FilePoller with poll interval %dms",
                  pollInterval == null ? 0 : pollInterval.toMillis()));
      return new PollBasedFilePoller(Paths.get(fileName), pollInterval);
    }
    logger.info("Setting up WatchService based FilePoller");
    return new WatchServiceBasedFilePoller(Paths.get(fileName));
  }

  private static AbstractFilePoller provideFilePollerBasedOnOs(
      String fileName, Duration pollInterval) throws IOException {
    boolean isMacOs = System.getProperty("os.name").toLowerCase().contains("mac");
    if (isMacOs) {
      logger.info("Running on macOS");
      return createFilePoller(fileName, FilePollerKind.POLL_BASED, pollInterval);
    } else {
      return createFilePoller(fileName, FilePollerKind.WATCHSERVICE_BASED, null);
    }
  }

  public FilePoller(String fileName) throws IOException {
    this(fileName, null);
  }

  FilePoller(String fileName, Duration pollInterval) throws IOException {
    if (pollInterval == null) {
      pollInterval = Duration.ofSeconds(60);
    }
    filePoller = provideFilePollerBasedOnOs(fileName, pollInterval);
  }

  // VisibleForTesting
  FilePoller(String fileName, FilePollerKind kind, Duration pollInterval) throws IOException {
    filePoller = createFilePoller(fileName, kind, pollInterval);
  }

  public boolean fileContentsUpdated() {
    return filePoller.fileContentsChanged();
  }

  public String getWatchedFilePath() {
    return filePoller.getWatchedFilePath();
  }

  @Override
  public void close() throws IOException {
    filePoller.close();
  }

  public enum FilePollerKind {
    POLL_BASED,
    WATCHSERVICE_BASED,
  }
}
