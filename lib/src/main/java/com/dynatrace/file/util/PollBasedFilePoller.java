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
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.CRC32;

class PollBasedFilePoller extends FilePoller {
  private static final Logger LOGGER = Logger.getLogger(PollBasedFilePoller.class.getName());
  private static final String LOG_MESSAGE_FAILED_FILE_READ = "Failed to read file %s. Error: %s";

  private final AtomicBoolean changedSinceLastInquiry = new AtomicBoolean(false);

  private final ScheduledFuture<?> worker;
  private final ScheduledExecutorService executorService;

  private final CRC32 crc32 = new CRC32();
  private Long prevChecksumValue = null;

  protected PollBasedFilePoller(Path filePath, Duration pollInterval) {
    super(filePath);
    if (pollInterval == null) {
      throw new IllegalArgumentException("Poll interval cannot be null");
    }

    executorService =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(null, r, "PollBasedFilePoller");
                t.setDaemon(true);
                return t;
              }
            });

    LOGGER.finer(() -> String.format("Polling every %dms", pollInterval.toMillis()));
    worker =
        executorService.scheduleAtFixedRate(
            this::poll, pollInterval.toNanos(), pollInterval.toNanos(), TimeUnit.NANOSECONDS);
  }

  @Override
  public boolean fileContentsUpdated() {
    // get the current value and reset to false
    return changedSinceLastInquiry.getAndSet(false);
  }

  private synchronized void poll() {
    Long latestChecksumValue = getChecksumValue();

    // | latest | previous | outcome                 |
    // | ------ | -------- | ----------------------- |
    // | null   | null     | do nothing              |
    // | null   | !null    | do nothing              |
    // | !null  | null     | update (no file before) |
    // | !null  | !null    | update if changed       |
    if (latestChecksumValue != null) {
      if (!prevChecksumValue.equals(latestChecksumValue)) {
        changedSinceLastInquiry.set(true);
      }
      prevChecksumValue = latestChecksumValue;
    }
  }

  private synchronized Long getChecksumValue() {
    crc32.reset();
    try {
      byte[] fileBytes = Files.readAllBytes(absoluteFilePath);
      crc32.update(fileBytes, 0, fileBytes.length);
    } catch (IOException e) {
      LOGGER.warning(() -> String.format(LOG_MESSAGE_FAILED_FILE_READ, absoluteFilePath, e));
      return null;
    }
    return crc32.getValue();
  }

  @Override
  public void close() {
    worker.cancel(true);
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(50, TimeUnit.MILLISECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      LOGGER.warning("failed to shut down poll based file poller: " + e);
    }
  }
}
