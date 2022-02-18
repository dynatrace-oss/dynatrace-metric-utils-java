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

class PollBasedFilePoller extends AbstractFilePoller {
  private final AtomicBoolean changedSinceLastPoll = new AtomicBoolean(false);
  private final ScheduledExecutorService scheduledExecutorService;
  private volatile long lastUpdatedAt = 0;
  private final ScheduledFuture<?> poller;

  protected PollBasedFilePoller(Path filePath, Duration pollInterval) {
    super(filePath);
    if (pollInterval == null) {
      throw new IllegalArgumentException("Poll interval cannot be null");
    }

    scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(null, r, "PollBasedFilePoller", 0);
                t.setDaemon(true);
                return t;
              }
            });
    poller =
        scheduledExecutorService.scheduleAtFixedRate(
            this::poll, pollInterval.toNanos(), pollInterval.toNanos(), TimeUnit.NANOSECONDS);

    // poll once initially and synchronously to make sure that the atomic data stores are
    // initialized before the end of the constructor.
    poll();
  }

  @Override
  public boolean fileContentsChanged() {
    // get the current value and reset to false
    return changedSinceLastPoll.getAndSet(false);
  }

  private void poll() {
    synchronized (this) {
      try {
        long modifiedAt = Files.getLastModifiedTime(absoluteFilePath).toMillis();
        long previouslyModifiedAt = lastUpdatedAt;
        lastUpdatedAt = modifiedAt;

        if (modifiedAt >= previouslyModifiedAt) {
          if (previouslyModifiedAt > 0) {
            changedSinceLastPoll.set(true);
          }
        }
      } catch (IOException e) {
        // One possible reason for this is that no read permissions exist on the file, in
        // which case the user should (try to) read his file anyway and handle any errors then.
        changedSinceLastPoll.set(true);
      }
    }
  }

  @Override
  public void close() {
    poller.cancel(false);
    scheduledExecutorService.shutdown();
  }
}
