package dev.sbutler.bitflask.common.io;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Provides utility functions dealing with files.
 */
public final class FilesHelper {

  private final ThreadFactory threadFactory;

  public FilesHelper(ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  /**
   * Read the directory at the provided path and collect the paths of all files within it.
   */
  public ImmutableList<Path> getFilePathsInDirectory(Path directoryPath) throws IOException {
    ImmutableList.Builder<Path> filePaths = new Builder<>();
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directoryPath)) {
      for (Path path : dirStream) {
        filePaths.add(path);
      }
    } catch (DirectoryIteratorException ex) {
      // I/O error encountered during the iteration, the cause is an IOException
      throw ex.getCause();
    }
    return filePaths.build();
  }

  /**
   * Concurrently retrieves the last modified time of each file provided.
   *
   * <p>The futures in the returned map are guaranteed to be done, i.e., {@link Future#isDone()}
   * will return {@code true}. Handling the state of the future ({@link Future#state()}) is the
   * responsibility of the caller.
   */
  public ImmutableMap<Path, Future<FileTime>> getLastModifiedTimeOfFiles(
      ImmutableList<Path> filePaths) throws InterruptedException {
    ImmutableMap.Builder<Path, Future<FileTime>> pathFileTimeFutures = new ImmutableMap.Builder<>();
    try (var scope =
        new StructuredTaskScope.ShutdownOnFailure("get-files-modified-time", threadFactory)) {
      for (Path path : filePaths) {
        Callable<FileTime> fileTimeCallable = () ->
            Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
        pathFileTimeFutures.put(path, scope.fork(fileTimeCallable));
      }
      scope.join();
    }
    return pathFileTimeFutures.build();

  }

  /**
   * Opens {@link FileChannel}s for the provided file {@link Path}s with the provided
   * {@link StandardOpenOption}s.
   *
   * <p>The futures in the returned map are guaranteed to be done, i.e., {@link Future#isDone()}
   * will return {@code true}. Handling the state of the future ({@link Future#state()}) is the
   * responsibility of the caller.
   */
  public ImmutableMap<Path, Future<FileChannel>> openFileChannels(
      ImmutableList<Path> filePaths, ImmutableSet<StandardOpenOption> fileChannelOptions)
      throws InterruptedException {
    ImmutableMap.Builder<Path, Future<FileChannel>> pathFutureFileChannelMap =
        new ImmutableMap.Builder<>();
    try (var scope =
        new StructuredTaskScope.ShutdownOnFailure("open-file-channels", threadFactory)) {
      for (Path path : filePaths) {
        Callable<FileChannel> pathCallable = () ->
            FileChannel.open(path, fileChannelOptions);
        pathFutureFileChannelMap.put(path, scope.fork(pathCallable));
      }
      scope.join();
    }
    return pathFutureFileChannelMap.build();
  }

  /**
   * Close the provided FileChannels while catching and ignoring {@link java.io.IOException}s and
   * proceeding to close the next.
   */
  public void closeFileChannelsBestEffort(ImmutableList<FileChannel> fileChannels) {
    fileChannels.forEach(fileChannel -> {
      try {
        fileChannel.close();
      } catch (IOException ignored) {
        // Best effort to close opened channels
      }
    });
  }
}
