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

import com.dynatrace.testutils.TempFiles;
import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilePollerTest {
  private static final boolean IS_MAC_OS = System.getProperty("os.name", "").toLowerCase().contains("mac");
  private TempFiles tf;
  
  @BeforeEach
  void setUp() throws IOException {
    tf = new TempFiles();
  }
  
  @AfterEach
  void cleanUp() throws IOException {
    tf.close();
    tf = null;
  }

  @Test
  void filePollerConstructorThrowsOnNonExistentFile() {
    final String nonExistentFile = TestUtils.generateNonExistentFilename();

    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getPollBased(nonExistentFile, Duration.ofMillis(50)));

    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getWatchServiceBased(nonExistentFile));
  }

  @Test
  void constructorThrowsOnNullDurationForPollBased() {
      assertThrows(
          IllegalArgumentException.class,
          () -> FilePollerFactory.getPollBased(tf.tempFile1Name(), null));
  }

  @Test
  void filePollerConstructorThrowsOnPassingAFolder() throws IOException {
    final Path tempDir = Files.createTempDirectory("temp");

    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getPollBased(tempDir.toString(), Duration.ofMillis(50)));

    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getWatchServiceBased(tempDir.toString()));
  }

  @Test
  void filePollerUpdatesOnChangePollBased() throws IOException {
      PollBasedFilePoller poller =
          FilePollerFactory.getPollBased(tf.tempFile1Name(), Duration.ofMillis(50));
      // wait for 5 millis so the first poll does not coincide with the file mod time
      await().atLeast(Duration.ofMillis(5)).until(() -> !poller.fileContentsUpdated());
      filePollerUpdatesOnChange(poller, tf.tempFile1());
  }

  @Test
  void filePollerUpdatesOnChangeWatchServiceBased() throws IOException {
    if (IS_MAC_OS) {
      System.out.println(
          "macOS does not support WatchService based pollers, skipping this test...");
      return;
    }
      filePollerUpdatesOnChange(
          FilePollerFactory.getWatchServiceBased(tf.tempFile1Name()), tf.tempFile1());
  }

  @Test
  void filePollerUpdatesOnFileMovePollBased() throws IOException {
      PollBasedFilePoller poller =
          FilePollerFactory.getPollBased(tf.tempFile1Name(), Duration.ofMillis(50));
      // wait for 5 millis so the first poll does not coincide with the file mod time
      await().atLeast(Duration.ofMillis(5)).until(() -> !poller.fileContentsUpdated());
      filePollerUpdatesOnFileMove(poller, tf);
  }

  @Test
  void filePollerUpdatesOnFileMoveWatchServiceBased() throws IOException {
    if (IS_MAC_OS) {
      System.out.println("Running on macOS, skipping this test...");
      return;
    }

      filePollerUpdatesOnFileMove(FilePollerFactory.getWatchServiceBased(tf.tempFile1Name()), tf);
  }

  @Test
  void filePollerUpdatesOnFileCopyPollBased() throws IOException {
      PollBasedFilePoller poller =
          FilePollerFactory.getPollBased(tf.tempFile1Name(), Duration.ofMillis(50));
      // wait for 5 millis so the first poll does not coincide with the file mod time
      await().atLeast(Duration.ofMillis(5)).until(() -> !poller.fileContentsUpdated());
      filePollerUpdatesOnFileCopy(poller, tf);
  }

  @Test
  void filePollerUpdatesOnFileCopyWatchServiceBased() throws IOException {
    if (IS_MAC_OS) {
      System.out.println("Running on macOS, skipping this test...");
      return;
    }

      filePollerUpdatesOnFileCopy(FilePollerFactory.getWatchServiceBased(tf.tempFile1Name()), tf);
  }

  // **** TEST HELPERS ****

  void filePollerUpdatesOnChange(FilePoller poller, Path tempFile)
      throws IOException {

    assertFalse(poller.fileContentsUpdated());

    Files.write(tempFile, "test file content".getBytes());

    // wait for non-blocking IO
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  void filePollerUpdatesOnFileMove(FilePoller poller, TempFiles tf)
      throws IOException {
    // set up the second file
    Files.write(tf.tempFile2(), "test file content for tempFile2".getBytes());

    // no changes to the first file yet
    assertFalse(poller.fileContentsUpdated());

    // move the second file into position, this should change the last modified timestamp
    Files.move(tf.tempFile2(), tf.tempFile1(), REPLACE_EXISTING);

    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  void filePollerUpdatesOnFileCopy(FilePoller poller, TempFiles tf) throws IOException {
    Files.write(tf.tempFile2(), "test file content for tempFile2".getBytes());

    assertFalse(poller.fileContentsUpdated());

    Files.copy(tf.tempFile2(), tf.tempFile1(), REPLACE_EXISTING);

    // wait for non-blocking IO
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }
}
