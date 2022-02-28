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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

class PollBasedFilePoller extends FilePoller {
  private final AtomicBoolean changedSinceLastInquiry = new AtomicBoolean(false);
  private static final Logger logger = Logger.getLogger(PollBasedFilePoller.class.getName());
  private static final FileTime ZERO_FILE_TIME = FileTime.from(0, TimeUnit.MILLISECONDS);
  private FileTime prevModifiedAt = ZERO_FILE_TIME;

  protected PollBasedFilePoller(Path filePath, Duration pollInterval) {
    super(filePath);
    if (pollInterval == null) {
      throw new IllegalArgumentException("Poll interval cannot be null");
    }

    ScheduledExecutorService scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(null, r, "PollBasedFilePoller");
                t.setDaemon(true);
                return t;
              }
            });
    logger.finer(() -> String.format("Polling every %dms", pollInterval.toMillis()));
    scheduledExecutorService.scheduleAtFixedRate(
        this::poll, pollInterval.toNanos(), pollInterval.toNanos(), TimeUnit.NANOSECONDS);

    // poll once initially and synchronously to make sure that the atomic data stores are
    // initialized before the end of the constructor.
    poll();
  }

  @Override
  public boolean fileContentsUpdated() {
    // get the current value and reset to false
    return changedSinceLastInquiry.getAndSet(false);
  }

  private synchronized void poll() {
    try {
      FileTime lastModifiedAt = Files.getLastModifiedTime(absoluteFilePath);

      if (lastModifiedAt.compareTo(prevModifiedAt) >= 0) {
        if (prevModifiedAt.compareTo(ZERO_FILE_TIME) != 0) {
          changedSinceLastInquiry.set(true);
        }
      }
      prevModifiedAt = lastModifiedAt;
    } catch (IOException e) {
      // One possible reason for this is that no read permissions exist on the file, in
      // which case the user should (try to) read the file anyway and handle any errors then.
      changedSinceLastInquiry.set(true);
      logger.warning(() -> String.format("Failed to read file %s; Error: %s", absoluteFilePath, e));
    }
  }
}
