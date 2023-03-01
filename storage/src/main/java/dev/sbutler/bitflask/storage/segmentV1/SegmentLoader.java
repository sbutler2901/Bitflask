package dev.sbutler.bitflask.storage.segmentV1;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.MoreFiles;
import com.google.mu.util.stream.BiStream;
import dev.sbutler.bitflask.common.concurrency.StructuredTaskScopeUtils;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.segmentV1.SegmentFile.Header;
import dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

final class SegmentLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final SegmentFactory segmentFactory;
  private final SegmentFile.Factory segmentFileFactory;
  private final ThreadFactory threadFactory;
  private final FilesHelper filesHelper;
  private final Path storeDirectoryPath;

  @Inject
  SegmentLoader(SegmentFactory segmentFactory, SegmentFile.Factory segmentFileFactory,
      ThreadFactory threadFactory, FilesHelper filesHelper,
      StorageConfigurations storageConfigurations) {
    this.threadFactory = threadFactory;
    this.segmentFactory = segmentFactory;
    this.segmentFileFactory = segmentFileFactory;
    this.filesHelper = filesHelper;
    this.storeDirectoryPath = storageConfigurations.getStorageStoreDirectoryPath();
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

      ImmutableList<Path> segmentFilePaths = getSegmentFilePaths();
      if (segmentFilePaths.isEmpty()) {
        logger.atInfo().log("No existing files found in segment store directory");
        return createManagedSegments(ImmutableList.of());
      }

      ImmutableList<Path> sortedSegmentFilePaths =
          sortFilePathsByLatestModifiedTimeFirst(segmentFilePaths);
      ImmutableMap<Path, FileChannel> pathFileChannelMap =
          openSegmentFileChannels(sortedSegmentFilePaths);
      ImmutableList<SegmentFile> segmentFiles = createSegmentFiles(pathFileChannelMap);
      ImmutableList<Segment> createdSegments = createSegmentsFromSegmentFiles(segmentFiles);

      logger.atInfo().log("Loaded [%d] preexisting segments", createdSegments.size());
      return createManagedSegments(createdSegments);
    } catch (SegmentLoaderException e) {
      throw e;
    } catch (Exception e) {
      throw new SegmentLoaderException("Failed to load existing segments", e);
    }
  }

  /**
   * Gets the file paths of any segments present in the segment store directory
   */
  @SuppressWarnings("UnstableApiUsage")
  private ImmutableList<Path> getSegmentFilePaths() {
    ImmutableList<Path> filePathsInDirectory;
    try {
      filePathsInDirectory = MoreFiles.listFiles(storeDirectoryPath);
    } catch (IOException e) {
      throw new SegmentLoaderException("Failed getting file paths in segment store directory", e);
    }
    return filePathsInDirectory.stream()
        .filter(SegmentFactory::isValidSegmentFilePath)
        .collect(toImmutableList());
  }

  private ImmutableList<Path> sortFilePathsByLatestModifiedTimeFirst(
      ImmutableList<Path> segmentFilePaths) {
    ImmutableMap<Path, Future<FileTime>> pathFileTimeFutures;
    try {
      pathFileTimeFutures = filesHelper.getLastModifiedTimeOfFiles(segmentFilePaths);
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while get file modified times", e);
    }

    ImmutableMap<Path, Throwable> failedFileTimePaths =
        StructuredTaskScopeUtils.getFailedFutureThrowablesFromMap(pathFileTimeFutures);

    if (!failedFileTimePaths.isEmpty()) {
      failedFileTimePaths.forEach((key, value) ->
          logger.atSevere().withCause(value)
              .log("Getting last modified time for file at path [%s] failed.", key));
      throw new SegmentLoaderException("File last modified times had failures");
    }

    ImmutableMap<Path, FileTime> successFileTimePaths =
        StructuredTaskScopeUtils.getSuccessfulFutureValuesFromMap(pathFileTimeFutures);

    // More recent modified first
    return BiStream.from(successFileTimePaths)
        .sortedByValues(Comparator.reverseOrder())
        .keys()
        .collect(toImmutableList());
  }

  private ImmutableMap<Path, FileChannel> openSegmentFileChannels(ImmutableList<Path> filePaths) {
    ImmutableMap<Path, Future<FileChannel>> pathFutureFileChannelMap;
    try {
      pathFutureFileChannelMap =
          filesHelper.openFileChannels(filePaths, segmentFactory.getFileChannelOptions());
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while opening file channels", e);
    }

    ImmutableMap<Path, FileChannel> openFileChannels =
        StructuredTaskScopeUtils.getSuccessfulFutureValuesFromMap(pathFutureFileChannelMap);

    ImmutableMap<Path, Throwable> failedFileChannels =
        StructuredTaskScopeUtils.getFailedFutureThrowablesFromMap(pathFutureFileChannelMap);

    if (!failedFileChannels.isEmpty()) {
      failedFileChannels.forEach((key, value) -> logger.atSevere().withCause(value)
          .log("Opening file channel for [%s] failed", key));
      filesHelper.closeFileChannelsBestEffort(openFileChannels.values().asList());
      throw new SegmentLoaderException("Failed opening file channels");
    }

    return openFileChannels;
  }

  /**
   * Creates corresponding SegmentFiles for all segments found in the segment store directory.
   */
  private ImmutableList<SegmentFile> createSegmentFiles(
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

  private static Header getHeaderFromFileChannel(FileChannel fileChannel) {
    try {
      return Header.readHeaderFromFileChannel(fileChannel);
    } catch (IOException e) {
      throw new SegmentLoaderException("Failed getting SegmentFile.Header from the FileChannel", e);
    }
  }

  private ImmutableList<Segment> createSegmentsFromSegmentFiles(
      ImmutableList<SegmentFile> segmentFiles) {
    ImmutableMap<SegmentFile, Future<Segment>> segmentFileFutureSegmentMap;
    try {
      segmentFileFutureSegmentMap = createSegmentFutures(segmentFiles);
    } catch (InterruptedException e) {
      throw new SegmentLoaderException("Interrupted while creating segments from segment files", e);
    }

    ImmutableMap<SegmentFile, Throwable> failedSegments =
        StructuredTaskScopeUtils.getFailedFutureThrowablesFromMap(segmentFileFutureSegmentMap);

    if (!failedSegments.isEmpty()) {
      failedSegments.forEach((key, value) -> logger.atSevere().withCause(value)
          .log("Creating segment from segment file with key [%d] failed", key.getSegmentFileKey()));
      segmentFiles.forEach(SegmentFile::close);
      throw new SegmentLoaderException("Failed creating segments from segment files");
    }

    ImmutableMap<SegmentFile, Segment> createdSegments =
        StructuredTaskScopeUtils.getSuccessfulFutureValuesFromMap(segmentFileFutureSegmentMap);

    return BiStream.from(createdSegments)
        .values()
        .collect(toImmutableList());
  }

  /**
   * Creates {@link Segment}s from the provided {@link SegmentFile}s.
   *
   * <p>The futures in the returned map are guaranteed to be done, i.e., {@link Future#isDone()}
   * will return {@code true}. Handling the state of the future ({@link Future#state()}) is the
   * responsibility of the caller.
   */
  private ImmutableMap<SegmentFile, Future<Segment>> createSegmentFutures(
      ImmutableList<SegmentFile> segmentFiles) throws InterruptedException {
    ImmutableMap.Builder<SegmentFile, Future<Segment>> segmentFileFutureSegmentMap = new ImmutableMap.Builder<>();
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("create-segments",
        threadFactory)) {
      for (SegmentFile segmentFile : segmentFiles) {
        Callable<Segment> segmentCallable = () -> segmentFactory.createSegmentFromFile(segmentFile);
        segmentFileFutureSegmentMap.put(segmentFile, scope.fork(segmentCallable));
      }
      scope.join();
    }
    return segmentFileFutureSegmentMap.build();
  }

  /**
   * Creates the final
   * {@link dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService.ManagedSegments} after
   * loading any segments.
   *
   * <p>If the provided list of loaded segments is empty a segment will be created to populated the
   * created ManagedSegments.
   */
  private ManagedSegments createManagedSegments(ImmutableList<Segment> loadedSegments) {
    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      try {
        writableSegment = segmentFactory.createSegment();
      } catch (IOException e) {
        throw new SegmentLoaderException("Failed creating a new segment for ManagedSegments", e);
      }
    } else {
      writableSegment = loadedSegments.get(0);
      loadedSegments = loadedSegments.subList(1, loadedSegments.size());
    }
    return new ManagedSegments(writableSegment, loadedSegments);
  }
}