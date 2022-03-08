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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.dynatrace.testutils.TempFiles;
import com.dynatrace.testutils.TestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PollBasedFilePollerTest {
  private TempFiles tf;
  private static final Duration POLL_INTERVAL = Duration.ofMillis(50);

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
        () -> FilePollerFactory.getPollBased(nonExistentFile, POLL_INTERVAL));
  }

  @Test
  void filePollerConstructorThrowsOnPassingAFolder() throws IOException {
    final Path tempDir = Files.createTempDirectory("temp");

    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getPollBased(tempDir.toString(), POLL_INTERVAL));
  }

  @Test
  void constructorThrowsOnNullDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getPollBased(tf.tempFile1Name(), null));
  }

  @Test
  void filePollerUpdatesOnChange() throws IOException {
    try (PollBasedFilePoller poller =
        FilePollerFactory.getPollBased(tf.tempFile1Name(), POLL_INTERVAL)) {
      FilePollerTestHelpers.filePollerUpdatesOnChange(poller, tf.tempFile1());
    }
  }

  @Test
  void filePollerUpdatesOnFileMove() throws IOException {
    try (PollBasedFilePoller poller =
        FilePollerFactory.getPollBased(tf.tempFile1Name(), POLL_INTERVAL)) {
      FilePollerTestHelpers.filePollerUpdatesOnFileMove(poller, tf);
    }
  }

  @Test
  void filePollerUpdatesOnFileCopy() throws IOException {
    try (PollBasedFilePoller poller =
        FilePollerFactory.getPollBased(tf.tempFile1Name(), POLL_INTERVAL)) {
      FilePollerTestHelpers.filePollerUpdatesOnFileCopy(poller, tf);
    }
  }

  @Test
  void pollingWorksAfterFileHasBeenDeletedAndCreated() throws IOException {
    try (PollBasedFilePoller poller =
        FilePollerFactory.getPollBased(tf.tempFile1Name(), POLL_INTERVAL)) {
      Path path = tf.tempFile1();

      Files.write(path, "Some test data".getBytes());

      await()
          .pollDelay(50, TimeUnit.MILLISECONDS)
          .then()
          .atMost(1, TimeUnit.SECONDS)
          .until(poller::fileContentsUpdated);

      Files.deleteIfExists(path);

      await()
          .pollDelay(Duration.ofMillis(50)) // wait to make sure the deletion operation is finished.
          .then()
          .atMost(150, TimeUnit.MILLISECONDS) // then check that no update has taken place.
          .until(
              () -> {
                boolean updated = poller.fileContentsUpdated();
                System.out.println("file contents updated: " + updated);
                return !updated;
              });

      // create the file again
      Files.createFile(path);
      await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);

      Files.write(path, "Some content".getBytes());
      await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);

      //      FilePollerTestHelpers.filePollerUpdatesOnlyOnCreateAndChangeNotOnDelete(
      //          poller, tf.tempFile1());
    }
  }
}
