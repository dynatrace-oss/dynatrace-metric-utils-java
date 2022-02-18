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

import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dynatrace.file.util.FilePoller.FilePollerKind.POLL_BASED;
import static com.dynatrace.file.util.FilePoller.FilePollerKind.WATCHSERVICE_BASED;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class FilePollerTest {
  Path tempDir;
  List<Path> tempFiles = new ArrayList<>();

  boolean isMacOs = System.getProperty("os.name").toLowerCase().contains("mac");

  @BeforeEach
  void setUp() throws IOException {
    cleanUp();
    tempDir = Files.createTempDirectory("tempdir");
    for (int i = 0; i < 2; i++) {
      tempFiles.add(Files.createTempFile(tempDir, "tempfile" + i, ".tmp"));
    }
  }

  @AfterEach
  void cleanUp() throws IOException {
    for (Path path : tempFiles) {
      Files.deleteIfExists(path);
    }
    tempFiles.clear();
    if (tempDir != null) {
      Files.deleteIfExists(tempDir);
    }
    tempDir = null;
  }

  @Test
  void filePollerConstructorThrowsOnNonExistentFile() {
    final String nonExistentFile = TestUtils.generateNonExistentFilename();

    assertThrows(
        IllegalArgumentException.class,
        () -> new FilePoller(nonExistentFile, POLL_BASED, Duration.ofMillis(50)));

    assertThrows(
        IllegalArgumentException.class,
        () -> new FilePoller(nonExistentFile, WATCHSERVICE_BASED, Duration.ofMillis(50)));
  }

  @Test
  void constructorThrowsOnNullDurationForPollBased() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FilePoller(tempFiles.get(0).toString(), POLL_BASED, null));
    // ignored for WatchService based impl.
    assertDoesNotThrow(() -> new FilePoller(tempFiles.get(0).toString(), WATCHSERVICE_BASED, null));
  }

  @Test
  void filePollerConstructorThrowsOnPassingAFolder() throws IOException {
    final Path tempDir = Files.createTempDirectory("temp");

    assertThrows(
        IllegalArgumentException.class,
        () -> new FilePoller(tempDir.toString(), POLL_BASED, Duration.ofMillis(50)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FilePoller(tempDir.toString(), WATCHSERVICE_BASED, Duration.ofMillis(50)));
  }

  void filePollerUpdatesOnChange(FilePoller poller) throws IOException {
    final Path tempFile = tempFiles.get(0);

    assertFalse(poller.fileContentsUpdated());

    Files.write(tempFile, "test file content".getBytes());

    // wait for non-blocking IO
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  @Test
  void filePollerUpdatesOnChangePollBased() throws IOException {
    try (FilePoller poller =
        new FilePoller(tempFiles.get(0).toString(), POLL_BASED, Duration.ofMillis(50))) {
      filePollerUpdatesOnChange(poller);
    }
  }

  @Test
  void filePollerUpdatesOnChangeWatchServiceBased() throws IOException {
    if (isMacOs) {
      System.out.println("Running on macOS, skipping this test...");
      return;
    }

    try (FilePoller poller =
        new FilePoller(tempFiles.get(0).toString(), WATCHSERVICE_BASED, null)) {
      filePollerUpdatesOnChange(poller);
    }
  }

  void filePollerUpdatesOnFileMove(FilePoller poller, Path tempFile1, Path tempFile2)
      throws IOException {
    // set up the second file
    Files.write(tempFile2, "test file content for tempFile2".getBytes());

    // no changes to the first file yet
    assertFalse(poller.fileContentsUpdated());

    // move the second file into position.
    Files.move(tempFile2, tempFile1, REPLACE_EXISTING);

    // wait for non-blocking io to be finished.
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  @Test
  void filePollerUpdatesOnFileMovePollBased() throws IOException {
    final Path tempFile1 = tempFiles.get(0);
    final Path tempFile2 = tempFiles.get(1);

    try (FilePoller poller =
        new FilePoller(tempFile1.toString(), POLL_BASED, Duration.ofMillis(50))) {
      filePollerUpdatesOnFileMove(poller, tempFile1, tempFile2);
    }
  }

  @Test
  void filePollerUpdatesOnFileMoveWatchServiceBased() throws IOException {
    if (isMacOs) {
      System.out.println("Running on macOS, skipping this test...");
      return;
    }

    final Path tempFile1 = tempFiles.get(0);
    final Path tempFile2 = tempFiles.get(1);

    try (FilePoller poller =
        new FilePoller(tempFile1.toString(), WATCHSERVICE_BASED, null)) {
      filePollerUpdatesOnFileMove(poller, tempFile1, tempFile2);
    }
  }

  void filePollerUpdatesOnFileCopy(FilePoller poller, Path tempFile1, Path tempFile2)
      throws IOException {
    Files.write(tempFile2, "test file content for tempFile2".getBytes());

    assertFalse(poller.fileContentsUpdated());

    Files.copy(tempFile2, tempFile1, REPLACE_EXISTING);

    // wait for non-blocking IO
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  @Test
  void filePollerUpdatesOnFileCopyPollBased() throws IOException {
    final Path tempFile1 = tempFiles.get(0);
    final Path tempFile2 = tempFiles.get(1);

    try (FilePoller poller =
        new FilePoller(tempFile1.toString(), POLL_BASED, Duration.ofMillis(50))) {
      filePollerUpdatesOnFileCopy(poller, tempFile1, tempFile2);
    }
  }

  @Test
  void filePollerUpdatesOnFileCopyWatchServiceBased() throws IOException {
    if (isMacOs) {
      System.out.println("Running on macOS, skipping this test...");
      return;
    }

    final Path tempFile1 = tempFiles.get(0);
    final Path tempFile2 = tempFiles.get(1);

    try (FilePoller poller =
        new FilePoller(tempFile1.toString(), WATCHSERVICE_BASED, null)) {
      filePollerUpdatesOnFileCopy(poller, tempFile1, tempFile2);
    }
  }
}
