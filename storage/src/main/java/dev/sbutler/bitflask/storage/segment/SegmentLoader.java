package dev.sbutler.bitflask.storage.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.mu.util.stream.GuavaCollectors.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.mu.util.stream.BiStream;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentFile.Header;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Future.State;
import javax.inject.Inject;

final class SegmentLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentFile.Factory segmentFileFactory;
  private final SegmentFactory segmentFactory;
  private final Path storeDirectoryPath;
  private final SegmentLoaderHelper segmentLoaderHelper;

  @Inject
  SegmentLoader(@StorageExecutorService ListeningExecutorService executorService,
      SegmentFile.Factory segmentFileFactory, SegmentFactory segmentFactory,
      SegmentLoaderHelper segmentLoaderHelper, StorageConfiguration storageConfiguration) {
    this.executorService = executorService;
    this.segmentFileFactory = segmentFileFactory;
    this.segmentFactory = segmentFactory;
    this.segmentLoaderHelper = segmentLoaderHelper;
    this.storeDirectoryPath = storageConfiguration.getStorageStoreDirectoryPath();
  }

  /**
   * Loads preexisting segments from the filesystem and initializes them for usage. Assumes the
   * directory for storing segments exists.
   *
   * @return the loaded segments
   * @throws SegmentLoaderException if an error occurs while loading the segments
   */
  public ManagedSegments loadExistingSegments() throws SegmentLoaderException {
    logger.atInfo().log("Loading any pre-existing Segments");

    try {
      boolean segmentStoreDirCreated = segmentFactory.createSegmentStoreDir();
      if (segmentStoreDirCreated) {
        logger.atInfo().log("Segment store directory created");
        return createManagedSegments(ImmutableList.of());
      }

      ImmutableList<Path> segmentFilePaths =
          segmentLoaderHelper.getFilePathsInDirectory(storeDirectoryPath);
      if (segmentFilePaths.isEmpty()) {
        logger.atInfo().log("No existing files found in segment store directory");
        return createManagedSegments(ImmutableList.of());
      }

      ImmutableList<Path> sortedSegmentFilePaths = sortFilePathsByLatestModifiedDatesFirst(
          segmentFilePaths);
      ImmutableMap<Path, FileChannel> pathFileChannelMap = openSegmentFileChannels(
          sortedSegmentFilePaths);
      ImmutableList<SegmentFile> segmentFiles = loadSegmentFiles(pathFileChannelMap);
      ImmutableList<Segment> createdSegments = createSegments(segmentFiles);
      logger.atInfo().log("Loaded [%d] preexisting segments", createdSegments.size());
      return createManagedSegments(createdSegments);
    } catch (SegmentLoaderException e) {
      throw e;
    } catch (Exception e) {
      throw new SegmentLoaderException("Failed to load existing segments", e);
    }
  }

  private ManagedSegments createManagedSegments(
      ImmutableList<Segment> loadedSegments) throws IOException {
    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      writableSegment = segmentFactory.createSegment();
    } else {
      writableSegment = loadedSegments.get(0);
      loadedSegments = loadedSegments.subList(1, loadedSegments.size());
    }

    return new ManagedSegments(writableSegment, loadedSegments);
  }

  ImmutableList<Path> sortFilePathsByLatestModifiedDatesFirst(
      ImmutableList<Path> segmentFilePaths) {
    ImmutableMap<Path, Future<FileTime>> pathFileTimeFutures;
    try {
      pathFileTimeFutures = segmentLoaderHelper.getLastModifiedTimeOfFiles(segmentFilePaths);
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while get file modified times", e);
    }

    ImmutableMap<Path, Throwable> failedFileTimePaths = BiStream.from(pathFileTimeFutures)
        .filterValues(f -> f.state() != State.SUCCESS)
        .mapValues(Future::exceptionNow)
        .collect(toImmutableMap());
    if (!failedFileTimePaths.isEmpty()) {
      failedFileTimePaths.forEach((key, value) ->
          logger.atSevere().withCause(value)
              .log("Getting last modified time for file at path [%s] failed.", key));
      throw new SegmentLoaderException("File last modified times had failures");
    }

    // More recent modified first
    return BiStream.from(pathFileTimeFutures)
        .mapValues(Future::resultNow)
        .sortedByValues(Comparator.reverseOrder())
        .keys()
        .collect(toImmutableList());
  }

  private ImmutableMap<Path, FileChannel> openSegmentFileChannels(ImmutableList<Path> filePaths) {
    ImmutableMap<Path, Future<FileChannel>> pathFutureFileChannelMap;
    try {
      pathFutureFileChannelMap = segmentLoaderHelper.openFileChannels(filePaths,
          segmentFactory.getFileChannelOptions());
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while opening segment file channels", e);
    }

    ImmutableMap<Path, FileChannel> openFileChannels =
        BiStream.from(pathFutureFileChannelMap)
            .filterValues(f -> f.state() == State.SUCCESS)
            .mapValues(Future::resultNow)
            .collect(toImmutableMap());

    ImmutableMap<Path, Throwable> failedFileChannels =
        BiStream.from(pathFutureFileChannelMap)
            .mapValues(Future::exceptionNow)
            .collect(toImmutableMap());

    failedFileChannels.forEach((key, value) -> logger.atSevere().withCause(value)
        .log("Opening FileChannel for [%s] failed", key));

    if (!failedFileChannels.isEmpty()) {
      segmentLoaderHelper.closeFileChannelsBestEffort(openFileChannels.values().asList());
      throw new SegmentLoaderException("Failed opening segment file channels");
    }

    return openFileChannels;
  }

  private ImmutableList<SegmentFile> loadSegmentFiles(
      ImmutableMap<Path, FileChannel> pathFileChannelMap) {
    ImmutableList.Builder<SegmentFile> segmentFiles = new ImmutableList.Builder<>();
    for (Entry<Path, FileChannel> entry : pathFileChannelMap.entrySet()) {
      Path path = entry.getKey();
      FileChannel fileChannel = entry.getValue();
      Header header = getHeaderFromFileChannel(fileChannel);
      SegmentFile segmentFile =
          segmentFileFactory.create(fileChannel, path, header);
      segmentFiles.add(segmentFile);
    }
    return segmentFiles.build();
  }

  private ImmutableList<Segment> createSegments(ImmutableList<SegmentFile> segmentFiles)
      throws IOException {
    ImmutableList.Builder<Callable<Segment>> segmentCallables = ImmutableList.builder();
    for (SegmentFile segmentFile : segmentFiles) {
      segmentCallables.add(() -> segmentFactory.createSegmentFromFile(segmentFile));
    }

    List<Future<Segment>> segmentFutures;
    try {
      segmentFutures = executorService.invokeAll(segmentCallables.build());
    } catch (InterruptedException e) {
      closeSegmentFiles(segmentFiles);
      throw new IOException("Creation of Segments interrupted", e);
    }

    ImmutableList.Builder<Segment> createdSegments = ImmutableList.builder();
    IOException segmentThrownException = null;
    for (int i = 0; i < segmentFutures.size(); i++) {
      try {
        createdSegments.add(segmentFutures.get(i).get());
      } catch (InterruptedException e) {
        segmentThrownException = new IOException(
            "At least one Segment failed to be opened");
        logger.atSevere().withCause(e)
            .log("Opening Segment for [%s] failed", segmentFiles.get(i).getSegmentFilePath());
      } catch (ExecutionException e) {
        segmentThrownException = new IOException(
            "At least one Segment failed to be opened");
        logger.atSevere().withCause(e.getCause())
            .log("Opening Segment for [%s] failed", segmentFiles.get(i).getSegmentFilePath());
      }
    }
    if (segmentThrownException != null) {
      closeSegmentFiles(segmentFiles);
      throw segmentThrownException;
    }

    return createdSegments.build();
  }

  private static Header getHeaderFromFileChannel(FileChannel fileChannel) {
    try {
      return Header.readHeaderFromFileChannel(fileChannel);
    } catch (IOException e) {
      throw new SegmentLoaderException("Failed getting SegmentFile.Header from the FileChannel", e);
    }
  }

  private static void closeSegmentFiles(ImmutableList<SegmentFile> segmentFiles) {
    segmentFiles.forEach(SegmentFile::close);
  }
}
