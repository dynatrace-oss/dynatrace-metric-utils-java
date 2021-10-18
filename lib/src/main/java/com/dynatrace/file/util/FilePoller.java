package com.dynatrace.file.util;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

class FilePoller {
  private final Path folder;
  private final Path absoluteFilename;
  private final WatchService watchService;

  public FilePoller(String fileName) throws IOException {
    Path path = Paths.get(fileName).toAbsolutePath();
    Path folder = path.getParent();

    // file needs to exist upon creation of the FilePoller.
    if (!Files.exists(path)) {
      throw new IllegalArgumentException(path.toString() + " does not exist.");
    }

    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException(path.toString() + " is a directory, a file is expected.");
    }

    this.folder = folder;
    this.absoluteFilename = path;
    this.watchService = FileSystems.getDefault().newWatchService();
    // watch the enclosing folder for changes. It is only possible to watch directories, not
    // just files.
    folder.register(watchService, ENTRY_MODIFY, ENTRY_CREATE);
  }

  /**
   * Check if the file contents have changed since the last poll. Returns true only if the file has
   * been modified (usually upon creation, including renaming a file to the watched location, or
   * when the contents of the file change). Does not return true if the file was deleted. Also
   * returns true, if a different file was moved to the watched location.
   *
   * @return true if the content of the file changed, or it was created. Will also return true, if
   *     the file was overwritten with the same data as before. Return false if the file did not
   *     change or the file was deleted.
   */
  public boolean fileContentsUpdated() {
    final List<WatchEvent<?>> watchEvents = poll();

    // watch events will contain all events for the watched folder
    for (WatchEvent<?> event : watchEvents) {
      Path filename = folder.resolve((Path) event.context()).toAbsolutePath();
      // only return true on the specific file that is watched.
      if (filename.compareTo(absoluteFilename) == 0) {
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

  public String getWatchedFilePath() {
    return absoluteFilename.toString();
  }
}
