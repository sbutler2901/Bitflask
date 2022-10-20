package dev.sbutler.bitflask.storage.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentFile.Header;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;

final class SegmentLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentFile.Factory segmentFileFactory;
  private final SegmentFactory segmentFactory;
  private final Path storeDirectoryPath;

  @Inject
  SegmentLoader(@StorageExecutorService ListeningExecutorService executorService,
      SegmentFile.Factory segmentFileFactory, SegmentFactory segmentFactory,
      StorageConfiguration storageConfiguration) {
    this.executorService = executorService;
    this.segmentFileFactory = segmentFileFactory;
    this.segmentFactory = segmentFactory;
    this.storeDirectoryPath = storageConfiguration.getStorageStoreDirectoryPath();
  }

  /**
   * Loads preexisting segments from the filesystem and initializes them for usage. Assumes the
   * directory for storing segments exists.
   *
   * @return the loaded segments
   * @throws IOException if an error occurs while loading the segments
   */
  public ManagedSegments loadExistingSegments() throws IOException {
    // TODO: use virtual threads
    logger.atInfo().log("Loading any pre-existing Segments");
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

    ImmutableList<Path> sortedSegmentFilePaths = sortFilePathsByLatestModifiedDatesFirst(
        segmentFilePaths);
    ImmutableList<FileChannel> segmentFileChannels = openSegmentFileChannels(
        sortedSegmentFilePaths);
    ImmutableList<SegmentFile> segmentFiles = loadSegmentFiles(segmentFileChannels,
        sortedSegmentFilePaths);
    ImmutableList<Segment> loadedSegments = loadSegments(segmentFiles);
    ImmutableList<Segment> sortedSegments = sortSegments(loadedSegments);
    logger.atInfo().log("Loaded [%d] preexisting segments", sortedSegments.size());
    return createManagedSegments(sortedSegments);
  }

  private ManagedSegments createManagedSegments(ImmutableList<Segment> loadedSegments)
      throws IOException {
    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      writableSegment = segmentFactory.createSegment();
    } else {
      writableSegment = loadedSegments.get(0);
      loadedSegments = loadedSegments.subList(1, loadedSegments.size());
    }

    return new ManagedSegments(writableSegment, loadedSegments);
  }

  /**
   * Reads the Segment directory and gets the file paths of all segments that exist within it.
   *
   * @return the segment file paths read from the Segment directory
   * @throws IOException if an error occurs reading the Segment directory
   */
  private ImmutableList<Path> getSegmentFilePaths() throws IOException {
    ImmutableList.Builder<Path> filePaths = new Builder<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(storeDirectoryPath)) {
      for (Path path : stream) {
        if (SegmentFactory.isValidSegmentFilePath(path)) {
          filePaths.add(path);
        }
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
    ImmutableList.Builder<Callable<FileChannel>> openFileChannelsCallables = ImmutableList.builder();
    for (Path path : filePaths) {
      openFileChannelsCallables.add(
          () -> FileChannel.open(path,
              ImmutableSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE)));
    }

    List<Future<FileChannel>> fileChannelFutures;
    try {
      fileChannelFutures = executorService.invokeAll(openFileChannelsCallables.build());
    } catch (InterruptedException e) {
      throw new IOException("Opening of FileChannels interrupted", e);
    }

    ImmutableList.Builder<FileChannel> openFileChannels = ImmutableList.builder();
    IOException fileChannelThrownException = null;
    for (int i = 0; i < fileChannelFutures.size(); i++) {
      try {
        openFileChannels.add(fileChannelFutures.get(i).get());
      } catch (InterruptedException e) {
        fileChannelThrownException = new IOException(
            "At least one FileChannel failed to be opened");
        logger.atSevere().withCause(e)
            .log("Opening FileChannel for [%s] failed", filePaths.get(i));
      } catch (ExecutionException e) {
        fileChannelThrownException = new IOException(
            "At least one FileChannel failed to be opened");
        logger.atSevere().withCause(e.getCause())
            .log("Opening FileChannel for [%s] failed", filePaths.get(i));
      }
    }
    if (fileChannelThrownException != null) {
      closeFileChannels(openFileChannels.build());
      throw fileChannelThrownException;
    }

    return openFileChannels.build();
  }

  private ImmutableList<SegmentFile> loadSegmentFiles(
      ImmutableList<FileChannel> openSegmentFileChannels, ImmutableList<Path> segmentFilePaths)
      throws IOException {
    ImmutableList.Builder<SegmentFile> segmentFiles = new ImmutableList.Builder<>();
    for (int i = 0; i < openSegmentFileChannels.size(); i++) {
      FileChannel segmentFileChannel = openSegmentFileChannels.get(i);
      Path segmentFilePath = segmentFilePaths.get(i);
      Header header = segmentFactory.readSegmentFileHeader(segmentFileChannel);
      SegmentFile segmentFile = segmentFileFactory.create(segmentFileChannel, segmentFilePath,
          header);
      segmentFiles.add(segmentFile);
    }
    return segmentFiles.build();
  }

  private ImmutableList<Segment> loadSegments(ImmutableList<SegmentFile> segmentFiles)
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

  /**
   * Sorts the segments via their file key in descending order.
   */
  private ImmutableList<Segment> sortSegments(ImmutableList<Segment> segments) {
    return segments.stream()
        .sorted((s0, s1) -> Integer.compare(s1.getSegmentFileKey(), s0.getSegmentFileKey()))
        .collect(toImmutableList());
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
}
