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
package com.dynatrace.testutils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFiles implements Closeable {
  private final Path directory;
  private final Path tempFile1;
  private final Path tempFile2;

  public String tempFile1Name() {
    return tempFile1.toString();
  }

  public String tempFile2Name() {
    return tempFile2.toString();
  }

  public Path tempFile1() {
    return tempFile1;
  }

  public Path tempFile2() {
    return tempFile2;
  }

  public TempFiles() throws IOException {
    directory = Files.createTempDirectory("tempdir");
    tempFile1 = Files.createTempFile(directory, "tempfile1_", ".tmp");
    tempFile2 = Files.createTempFile(directory, "tempfile2_", ".tmp");
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(tempFile1);
    Files.deleteIfExists(tempFile2);
    Files.deleteIfExists(directory);
  }
}
