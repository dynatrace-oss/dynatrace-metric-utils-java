package com.dynatrace.file.util;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

class WatchServiceBasedFilePoller extends AbstractFilePoller {
  private final Path folder;
  private final WatchService watchService;

  public WatchServiceBasedFilePoller(Path absoluteFilename) throws IOException {
    super(absoluteFilename);
    folder = absoluteFilename.getParent();

    watchService = FileSystems.getDefault().newWatchService();
    folder.register(
        watchService,
        new WatchEvent.Kind[] {ENTRY_MODIFY, ENTRY_CREATE},
        SensitivityWatchEventModifier.HIGH);
  }

  @Override
  public boolean fileContentsChanged() {
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

  @Override
  public void close() throws IOException {
    watchService.close();
  }
}
