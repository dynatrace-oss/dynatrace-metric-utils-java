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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dynatrace.testutils.TempFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

class FilePollerTestHelpers {
  // **** TEST HELPERS ****

  static void filePollerUpdatesOnChange(FilePoller poller, Path tempFile) throws IOException {
    assertFalse(poller.fileContentsUpdated());

    Files.write(tempFile, "test file content".getBytes());

    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  static void filePollerUpdatesOnFileMove(FilePoller poller, TempFiles tf) throws IOException {
    // set up the second file
    Files.write(tf.tempFile2(), "test file content for tempFile2".getBytes());

    // no changes to the first file yet
    assertFalse(poller.fileContentsUpdated());

    // move the second file into position, this should change the last modified timestamp
    Files.move(tf.tempFile2(), tf.tempFile1(), REPLACE_EXISTING);

    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  static void filePollerUpdatesOnFileCopy(FilePoller poller, TempFiles tf) throws IOException {
    Files.write(tf.tempFile2(), "test file content for tempFile2".getBytes());

    assertFalse(poller.fileContentsUpdated());

    Files.copy(tf.tempFile2(), tf.tempFile1(), REPLACE_EXISTING);

    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
    assertFalse(poller.fileContentsUpdated());
  }

  static void filePollerUpdatesOnlyOnCreateAndChangeNotOnDelete(FilePoller poller, Path path)
      throws IOException {
    Files.write(path, "Some test data".getBytes());

    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);

    Files.deleteIfExists(path);

    await()
        .pollDelay(Duration.ofMillis(100)) // wait to make sure the deletion operation is finished.
        .atMost(150, TimeUnit.MILLISECONDS) // then check that no update has taken place.
        .until(() -> !poller.fileContentsUpdated());

    // create the file again
    Files.createFile(path);
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);

    Files.write(path, "Some content".getBytes());
    await().atMost(1, TimeUnit.SECONDS).until(poller::fileContentsUpdated);
  }
}
