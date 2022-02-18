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

  /**
   * Check if the file contents have changed since the last poll. Returns true only if the file has
   * been modified (usually upon creation, including renaming a file to the watched location, or
   * when the contents of the file change). Does not return true if the file was deleted. Also
   * returns true, if a different file was moved to the watched location.
   *
   * @return true if the content of the file changed, or it was created. Will also return true, if
   *     the file was overwritten with the same data as before. Return false if the file did not
   *     change or the file was deleted.
   */
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
