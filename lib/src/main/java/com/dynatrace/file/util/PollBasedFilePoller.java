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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

class PollBasedFilePoller extends FilePoller {
  private final AtomicBoolean changedSinceLastInquiry = new AtomicBoolean(false);
  private static final Logger logger = Logger.getLogger(PollBasedFilePoller.class.getName());

  private long prevModMillis = 0;
  private String prevChecksum = "";

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

    logger.finer(() -> String.format("Polling every %dms", pollInterval.toMillis()));
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
    try {
      prevChecksum = getFileChecksum();
      prevModMillis =
          Files.getLastModifiedTime(absoluteFilePath, LinkOption.NOFOLLOW_LINKS).toMillis();
    } catch (IOException e) {
      logger.warning(
          () ->
              String.format(
                  "Failed to read file %s, stopping polling. Error: %s", absoluteFilePath, e));
      worker.cancel(true);
    }
  }

  private synchronized void poll() {
    try {
      // check if the filetime has changed.
      long lastModifiedMillis = Files.getLastModifiedTime(absoluteFilePath).toMillis();
      if (lastModifiedMillis > prevModMillis) {
        prevModMillis = lastModifiedMillis;
        changedSinceLastInquiry.set(true);
        return;
      }

      // if the filetime hasn't changed, check if the hash has changed.
      String fileChecksum = getFileChecksum();
      if (!fileChecksum.equals(prevChecksum)) {
        prevChecksum = fileChecksum;
        prevModMillis = lastModifiedMillis;
        changedSinceLastInquiry.set(true);
      }
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

  private synchronized String getFileChecksum() throws IOException {
    // Get file input stream for reading the file content
    try (FileInputStream fis = new FileInputStream(absoluteFilePath.toFile())) {

      // Create byte array to read data in chunks
      byte[] byteArray = new byte[1024];
      int bytesCount;

      // Read file data and update in message digest
      while ((bytesCount = fis.read(byteArray)) != -1) {
        md5.update(byteArray, 0, bytesCount);
      }
    }

    // Get the hash's bytes
    byte[] bytes = md5.digest();

    // This bytes[] has bytes in decimal format, convert it to hexadecimal format
    StringBuilder sb = new StringBuilder();
    for (byte aByte : bytes) {
      sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
    }

    // return complete hash
    return sb.toString();
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
      logger.warning("failed to shut down poll based file poller: " + e);
    }
  }
}
