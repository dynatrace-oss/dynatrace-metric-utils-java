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

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

class FilePollerFactory {
  private static final Logger logger = Logger.getLogger(FilePollerFactory.class.getName());
  private static final boolean IS_MAC_OS =
      System.getProperty("os.name", "").toLowerCase().contains("mac");

  private FilePollerFactory() {}

  /**
   * Creates the default {@link FilePoller} based on the current OS.
   *
   * @implNote On Linux and Windows, * the poll mechanism is based on the {@link
   *     java.nio.file.WatchService WatchService}. For macOS, * where there is no native
   *     implementation for the {@link java.nio.file.WatchService WatchService} * bindings, this
   *     method provides a {@link PollBasedFilePoller}, which polls the file of interest *
   *     periodically (according to the {@code pollInterval}).
   * @param fileName The name of the file to be watched.
   * @param pollInterval The interval in which the {@link PollBasedFilePoller} polls for changes.
   *     Only applicable on macOS. Will default to 60s if {@code null} is passed.
   * @return An object that implements the abstract methods on {@link FilePoller}.
   * @throws IOException if the initialization of the {@link WatchServiceBasedFilePoller} is not
   *     successful.
   */
  static FilePoller getDefault(String fileName, Duration pollInterval) throws IOException {
    if (IS_MAC_OS) {
      logger.fine("Running on macOS");
      if (pollInterval == null) {
        pollInterval = Duration.ofSeconds(60);
      }
      return getPollBased(fileName, pollInterval);
    } else {
      return getWatchServiceBased(fileName);
    }
  }

  static PollBasedFilePoller getPollBased(String fileName, Duration pollInterval) {
    if (logger.isLoggable(Level.INFO) && pollInterval != null) {
      logger.info(
          String.format(
              "Setting up poll-based FilePoller with poll interval %dms", pollInterval.toMillis()));
    }
    return new PollBasedFilePoller(Paths.get(fileName), pollInterval);
  }

  static WatchServiceBasedFilePoller getWatchServiceBased(String fileName) throws IOException {
    logger.info("Setting up WatchService-based FilePoller");
    return new WatchServiceBasedFilePoller(Paths.get(fileName));
  }
}
