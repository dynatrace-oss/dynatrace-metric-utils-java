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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

class PollBasedFilePoller extends FilePoller {
  private static final Logger LOGGER = Logger.getLogger(PollBasedFilePoller.class.getName());
  private static final String LOG_MESSAGE_FAILED_FILE_READ = "Failed to read file %s. Error: %s";

  private final AtomicBoolean changedSinceLastInquiry = new AtomicBoolean(false);
  private byte[] prevChecksumBytes = null;

  private final ScheduledFuture<?> worker;
  private final ScheduledExecutorService executorService;

  private MessageDigest md5;

  protected PollBasedFilePoller(Path filePath, Duration pollInterval) {
    super(filePath);
    if (pollInterval == null) {
      throw new IllegalArgumentException("Poll interval cannot be null");
    }

    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ignored) {
      // ignore, since this is not something dependent on the code. MD5 should always be there.
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

    initialPoll();
  }

  @Override
  public boolean fileContentsUpdated() {
    // get the current value and reset to false
    return changedSinceLastInquiry.getAndSet(false);
  }

  private synchronized void initialPoll() {
    prevChecksumBytes = getChecksumBytes();
  }

  private synchronized void poll() {
    byte[] latestChecksumBytes = getChecksumBytes();

    if (!Arrays.equals(latestChecksumBytes, prevChecksumBytes)) {
      prevChecksumBytes = latestChecksumBytes;

      // The file did exist before (prevChecksumBytes != null) but doesn't anymore
      // (latestChecksumBytes == null).
      // This code here is only reached when prevChecksumBytes != latestChecksumBytes.
      // This means the file has been deleted since the last poll, which should not trigger a
      // state change.
      if (latestChecksumBytes != null) {
        changedSinceLastInquiry.set(true);
      }
    }
  }

  private synchronized byte[] getChecksumBytes() {
    byte[] bytes = null;
    try {
      bytes = md5.digest(Files.readAllBytes(absoluteFilePath));
    } catch (IOException e) {
      LOGGER.warning(() -> String.format(LOG_MESSAGE_FAILED_FILE_READ, absoluteFilePath, e));
    } finally {
      md5.reset();
    }
    return bytes;
  }

  @Override
  public void close() throws IOException {
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
