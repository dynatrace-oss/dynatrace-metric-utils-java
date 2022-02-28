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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

class WatchServiceBasedFilePoller extends FilePoller {
  private final Path folder;
  private final WatchService watchService;

  public WatchServiceBasedFilePoller(Path absoluteFilename) throws IOException {
    super(absoluteFilename);
    folder = absoluteFilename.getParent();

    watchService = FileSystems.getDefault().newWatchService();
    folder.register(watchService, ENTRY_MODIFY, ENTRY_CREATE);
  }

  @Override
  public boolean fileContentsUpdated() {
    final List<WatchEvent<?>> watchEvents = poll();

    // watch events will contain all events for the watched folder
    for (WatchEvent<?> event : watchEvents) {
      Path filename = folder.resolve((Path) event.context()).toAbsolutePath();
      // only return true on the specific file that is watched.
      if (filename.compareTo(absoluteFilePath) == 0) {
        return true;
      }
    }
    return false;
  }

  private List<WatchEvent<?>> poll() {
    final WatchKey watchKey = watchService.poll();
    if (watchKey == null) {
      return Collections.emptyList();
    }

    final List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
    watchKey.reset();
    return watchEvents;
  }
}
