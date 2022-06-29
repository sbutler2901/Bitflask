package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;

class SegmentLoaderImpl implements SegmentLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ExecutorService executorService;
  private final SegmentFileFactory segmentFileFactory;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLoaderImpl(@StorageExecutorService ExecutorService executorService,
      SegmentFileFactory segmentFileFactory, SegmentFactory segmentFactory) {
    this.executorService = executorService;
    this.segmentFileFactory = segmentFileFactory;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public ImmutableList<Segment> loadExistingSegments() throws IOException {
    ImmutableList<Path> segmentFilePaths = getSegmentFilePaths();
    if (segmentFilePaths.isEmpty()) {
      logger.atInfo().log("No existing files found in segment store directory");
      return ImmutableList.of();
    }

    ImmutableList<Path> sortedSegmentFilePaths = sortFilePathsByLatestModifiedDatesFirst(
        segmentFilePaths);
    ImmutableList<FileChannel> segmentFileChannels = openSegmentFileChannels(
        sortedSegmentFilePaths);
    ImmutableList<SegmentFile> segmentFiles = loadSegmentFiles(segmentFileChannels,
        sortedSegmentFilePaths);
    ImmutableList<Segment> segments = loadSegments(segmentFiles);
    updateSegmentFactorySegmentStartIndex(segments);
    logger.atInfo().log("Loaded [%d] preexisting segments", segments.size());
    return segments;
  }

  /**
   * Reads the Segment directory and gets the file paths of all segments that exist within it.
   *
   * @return the segment file paths read from the Segment directory
   * @throws IOException if an error occurs reading the Segment directory
   */
  private ImmutableList<Path> getSegmentFilePaths() throws IOException {
    Path segmentStoreDirPath = segmentFactory.getSegmentStoreDirPath();
    ImmutableList.Builder<Path> filePaths = new Builder<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(segmentStoreDirPath)) {
      for (Path entry : stream) {
        filePaths.add(entry);
      }
    } catch (DirectoryIteratorException ex) {
      // I/O error encountered during the iteration, the cause is an IOException
      throw ex.getCause();
    }
    return filePaths.build();
  }

  private ImmutableList<Path> sortFilePathsByLatestModifiedDatesFirst(
      ImmutableList<Path> segmentFilePaths)
      throws IOException {
    // More recent modified first
    ImmutableSortedMap.Builder<FileTime, Path> pathFileTimeMapBuilder =
        new ImmutableSortedMap.Builder<>(Comparator.reverseOrder());

    for (Path path : segmentFilePaths) {
      FileTime pathFileTime = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
      pathFileTimeMapBuilder.put(pathFileTime, path);
    }
    return pathFileTimeMapBuilder.build().values().asList();
  }

  private ImmutableList<FileChannel> openSegmentFileChannels(ImmutableList<Path> filePaths)
      throws IOException {
    ImmutableList.Builder<FileChannel> openFilesBuilder = new ImmutableList.Builder<>();
    try {
      for (Path path : filePaths) {
        FileChannel fileChannel = FileChannel.open(path, Set.of(StandardOpenOption.READ,
            StandardOpenOption.WRITE));
        openFilesBuilder.add(fileChannel);
      }
    } catch (IOException e) {
      closeFileChannels(openFilesBuilder.build());
      throw e;
    }
    return openFilesBuilder.build();
  }

  private ImmutableList<SegmentFile> loadSegmentFiles(
      ImmutableList<FileChannel> openSegmentFileChannels, ImmutableList<Path> segmentFilePaths) {
    ImmutableList.Builder<SegmentFile> segmentFiles = new ImmutableList.Builder<>();
    for (int i = 0; i < openSegmentFileChannels.size(); i++) {
      FileChannel segmentFileChannel = openSegmentFileChannels.get(i);
      Path segmentFilePath = segmentFilePaths.get(i);
      int segmentKey = segmentFactory.getSegmentKeyFromPath(segmentFilePath);
      SegmentFile segmentFile = segmentFileFactory.create(segmentFileChannel, segmentFilePath,
          segmentKey);
      segmentFiles.add(segmentFile);
    }
    return segmentFiles.build();
  }

  private ImmutableList<Segment> loadSegments(ImmutableList<SegmentFile> segmentFiles)
      throws IOException {
    List<Callable<Segment>> segmentCallables = new ArrayList<>();
    for (SegmentFile segmentFile : segmentFiles) {
      segmentCallables.add(() -> segmentFactory.createSegmentFromFile(segmentFile));
    }
    try {
      List<Future<Segment>> segmentFutures = executorService.invokeAll(segmentCallables);
      ImmutableList.Builder<Segment> segments = new ImmutableList.Builder<>();
      for (Future<Segment> segmentFuture : segmentFutures) {
        segments.add(segmentFuture.get());
      }
      return segments.build();
    } catch (InterruptedException | ExecutionException e) {
      closeSegmentFiles(segmentFiles);
      throw new IOException("Failed to load previous segments", e);
    }
  }

  private void closeFileChannels(ImmutableList<FileChannel> fileChannels) {
    fileChannels.forEach(fileChannel -> {
      try {
        fileChannel.close();
      } catch (IOException ignored) {
        // Best effort to close opened channels
      }
    });
  }

  private void closeSegmentFiles(ImmutableList<SegmentFile> segmentFiles) {
    segmentFiles.forEach(SegmentFile::close);
  }

  private void updateSegmentFactorySegmentStartIndex(ImmutableList<Segment> segments) {
    int latestSegmentKey = segments.get(0).getSegmentFileKey();
    segmentFactory.setSegmentStartKey(++latestSegmentKey);
  }
}
