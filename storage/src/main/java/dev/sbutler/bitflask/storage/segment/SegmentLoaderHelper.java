package dev.sbutler.bitflask.storage.segment;

import static com.google.mu.util.stream.GuavaCollectors.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.mu.util.stream.BiStream;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageThreadFactory;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

final class SegmentLoaderHelper {

  private final StorageThreadFactory storageThreadFactory;

  @Inject
  SegmentLoaderHelper(StorageThreadFactory storageThreadFactory) {
    this.storageThreadFactory = storageThreadFactory;
  }

  /**
   * Read the directory at the provided path and collect the paths of all files within it.
   */
  ImmutableList<Path> getFilePathsInDirectory(Path directoryPath) throws IOException {
    ImmutableList.Builder<Path> filePaths = new Builder<>();
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directoryPath)) {
      dirStream.forEach(path -> {
        if (SegmentFactory.isValidSegmentFilePath(path)) {
          filePaths.add(path);
        }
      });
    } catch (DirectoryIteratorException ex) {
      // I/O error encountered during the iteration, the cause is an IOException
      throw ex.getCause();
    }
    return filePaths.build();
  }

  /**
   * Concurrently retrieves the last modified time of each file provided.
   */
  ImmutableMap<Path, FileTime> getLastModifiedTimeOfFiles(ImmutableList<Path> filePaths)
      throws InterruptedException, ExecutionException {
    ImmutableMap<Path, Future<FileTime>> pathFileTimeFutures;
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("loader-sort-modified-time",
        storageThreadFactory)) {
      ImmutableMap.Builder<Path, Future<FileTime>> builder = new ImmutableMap.Builder<>();
      for (Path path : filePaths) {
        Callable<FileTime> fileTimeCallable = () ->
            Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
        builder.put(path, scope.fork(fileTimeCallable));
      }
      pathFileTimeFutures = builder.build();
      scope.join();
      scope.throwIfFailed();
    }

    return BiStream.from(pathFileTimeFutures)
        .mapValues(Future::resultNow)
        .collect(toImmutableMap());
  }

  /**
   * Close the provided FileChannels while catching and ignoring {@link java.io.IOException}s and
   * proceeding to close the next.
   */
  void closeFileChannelsBestEffort(ImmutableList<FileChannel> fileChannels) {
    fileChannels.forEach(fileChannel -> {
      try {
        fileChannel.close();
      } catch (IOException ignored) {
        // Best effort to close opened channels
      }
    });
  }
}
