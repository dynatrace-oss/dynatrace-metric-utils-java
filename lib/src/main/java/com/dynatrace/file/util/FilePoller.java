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

import java.nio.file.Files;
import java.nio.file.Path;

abstract class FilePoller {
  protected final Path absoluteFilePath;

  protected FilePoller(Path filename) {
    if (filename == null) {
      throw new IllegalArgumentException("filename cannot be null.");
    }

    Path absPath = filename.toAbsolutePath();
    if (Files.isDirectory(absPath)) {
      throw new IllegalArgumentException(String.format("Passed path is a directory: %s", absPath));
    }

    if (!Files.exists(absPath)) {
      throw new IllegalArgumentException(String.format("File does not exist: %s", absPath));
    }

    this.absoluteFilePath = absPath;
  }

  public abstract boolean fileContentsUpdated();

  public String getWatchedFilePath() {
    return absoluteFilePath.toString();
  }
}
