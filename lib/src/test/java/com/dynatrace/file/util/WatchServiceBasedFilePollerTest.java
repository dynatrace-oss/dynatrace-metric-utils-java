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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.dynatrace.testutils.TempFiles;
import com.dynatrace.testutils.TestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatchServiceBasedFilePollerTest {
  private static final boolean IS_MAC_OS =
      System.getProperty("os.name", "").toLowerCase().contains("mac");
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
        () -> FilePollerFactory.getWatchServiceBased(nonExistentFile));
  }

  @Test
  void filePollerConstructorThrowsOnPassingAFolder() throws IOException {
    final Path tempDir = Files.createTempDirectory("temp");

    assertThrows(
        IllegalArgumentException.class,
        () -> FilePollerFactory.getWatchServiceBased(tempDir.toString()));
  }

  @Test
  void filePollerUpdatesOnChange() throws IOException {
    assumeFalse(
        IS_MAC_OS, "macOS does not support WatchService based pollers, skipping this test...");

    try (WatchServiceBasedFilePoller poller =
        FilePollerFactory.getWatchServiceBased(tf.tempFile1Name())) {
      FilePollerTestHelpers.filePollerUpdatesOnChange(poller, tf.tempFile1());
    }
  }

  @Test
  void filePollerUpdatesOnFileMove() throws IOException {
    assumeFalse(
        IS_MAC_OS, "macOS does not support WatchService based pollers, skipping this test...");

    try (WatchServiceBasedFilePoller poller =
        FilePollerFactory.getWatchServiceBased(tf.tempFile1Name())) {
      FilePollerTestHelpers.filePollerUpdatesOnFileMove(poller, tf);
    }
  }

  @Test
  void filePollerUpdatesOnFileCopy() throws IOException {
    assumeFalse(
        IS_MAC_OS, "macOS does not support WatchService based pollers, skipping this test...");

    try (WatchServiceBasedFilePoller poller =
        FilePollerFactory.getWatchServiceBased(tf.tempFile1Name())) {
      FilePollerTestHelpers.filePollerUpdatesOnFileCopy(poller, tf);
    }
  }

  @Test
  void pollingWorksAfterFileHasBeenDeletedAndCreated() throws IOException {
    try (WatchServiceBasedFilePoller poller =
        FilePollerFactory.getWatchServiceBased(tf.tempFile1Name())) {
      FilePollerTestHelpers.filePollerUpdatesOnlyOnCreateAndChangeNotOnDelete(
          poller, tf.tempFile1());
    }
  }
}
