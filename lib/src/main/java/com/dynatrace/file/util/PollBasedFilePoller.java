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
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

class PollBasedFilePoller extends FilePoller {
  private final AtomicBoolean changedSinceLastInquiry = new AtomicBoolean(false);
  private static final Logger logger = Logger.getLogger(PollBasedFilePoller.class.getName());

  private long prevModMillis = 0;

  private final ScheduledFuture<?> worker;

  protected PollBasedFilePoller(Path filePath, Duration pollInterval) {
    super(filePath);
    if (pollInterval == null) {
      throw new IllegalArgumentException("Poll interval cannot be null");
    }

    ScheduledExecutorService executorService =
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
    worker =
        executorService.scheduleAtFixedRate(
            this::poll, pollInterval.toNanos(), pollInterval.toNanos(), TimeUnit.NANOSECONDS);

    try {
      // initially poll once and wait for it to complete.
      executorService.invokeAll(
          Arrays.asList(Executors.callable(this::poll)), 5, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ignored) {
    }
  }

  @Override
  public boolean fileContentsUpdated() {
    // get the current value and reset to false
    return changedSinceLastInquiry.getAndSet(false);
  }

  private synchronized void poll() {
    try {
      long modifiedAtMillis = Files.getLastModifiedTime(absoluteFilePath).toMillis();
      if (modifiedAtMillis > prevModMillis && prevModMillis > 0) {
        changedSinceLastInquiry.set(true);
      }
      prevModMillis = modifiedAtMillis;
    } catch (IOException e) {
      // One possible reason for this is that no read permissions exist on the file, in
      // which case the user should (try to) read the file anyway and handle any errors then.
      changedSinceLastInquiry.set(true);
      logger.warning(
          () ->
              String.format(
                  "Failed to read file %s, stopping polling. Error: %s", absoluteFilePath, e));
      worker.cancel(true);
    }
  }
}
