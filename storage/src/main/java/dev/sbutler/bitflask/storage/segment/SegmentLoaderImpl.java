package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
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
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.slf4j.Logger;

class SegmentLoaderImpl implements SegmentLoader {

  @InjectStorageLogger
  Logger logger;

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLoaderImpl(@StorageExecutorService ExecutorService executorService,
      SegmentFactory segmentFactory) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public Deque<Segment> loadExistingSegments() throws IOException {
    Deque<Segment> loadedSegments = new ConcurrentLinkedDeque<>();
    List<Path> segmentFilePaths = getSegmentFilePaths();
    if (segmentFilePaths.isEmpty()) {
      return loadedSegments;
    }

    sortFilePathsByModifiedDate(segmentFilePaths);
    List<FileChannel> openSegmentFileChannels = openSegmentFiles(segmentFilePaths);
    List<SegmentFile> segmentFiles = loadSegmentFiles(openSegmentFileChannels, segmentFilePaths);
    List<Segment> segments = loadSegments(segmentFiles);
    updateSegmentFactorySegmentStartIndex(segments);
    loadedSegments.addAll(segments);
    return loadedSegments;
  }

  private List<Path> getSegmentFilePaths() throws IOException {
    Path segmentStoreDirPath = segmentFactory.getSegmentStoreDirPath();
    List<Path> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(segmentStoreDirPath)) {
      for (Path entry : stream) {
        result.add(entry);
      }
    } catch (DirectoryIteratorException ex) {
      // I/O error encountered during the iteration, the cause is an IOException
      throw ex.getCause();
    }
    return result;
  }

  private void sortFilePathsByModifiedDate(List<Path> segmentFilePaths) throws IOException {
    Map<Path, FileTime> pathFileTimeMap = new HashMap<>();
    for (Path path : segmentFilePaths) {
      FileTime pathFileTime = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
      pathFileTimeMap.put(path, pathFileTime);
    }
    // More recent modified first
    segmentFilePaths.sort(
        (path0, path1) -> pathFileTimeMap.get(path1).compareTo(pathFileTimeMap.get(path0)));
  }

  private List<FileChannel> openSegmentFiles(List<Path> filePaths) throws IOException {
    List<FileChannel> openFiles = new ArrayList<>();
    for (Path path : filePaths) {
      FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
      openFiles.add(fileChannel);
    }
    return openFiles;
  }

  private List<SegmentFile> loadSegmentFiles(List<FileChannel> openSegmentFileChannels,
      List<Path> segmentFilePaths) {
    List<SegmentFile> segmentFiles = new ArrayList<>();
    for (int i = 0; i < openSegmentFileChannels.size(); i++) {
      FileChannel segmentFileChannel = openSegmentFileChannels.get(i);
      Path segmentFilePath = segmentFilePaths.get(i);
      String segmentKey = segmentFactory.getSegmentKeyFromPath(segmentFilePath);
      SegmentFile segmentFile = new SegmentFile(segmentFileChannel, segmentFilePath, segmentKey);
      segmentFiles.add(segmentFile);
    }
    return segmentFiles;
  }

  private List<Segment> loadSegments(List<SegmentFile> segmentFiles) throws IOException {
    List<Callable<Segment>> segmentCallables = new ArrayList<>();
    for (SegmentFile segmentFile : segmentFiles) {
      segmentCallables.add(() -> new SegmentImpl(segmentFile));
    }
    try {
      List<Future<Segment>> segmentFutures = executorService.invokeAll(segmentCallables);
      List<Segment> segments = new ArrayList<>();
      for (Future<Segment> segmentFuture : segmentFutures) {
        segments.add(segmentFuture.get());
      }
      return segments;
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException("Failed to load previous segments", e);
    }
  }

  private void updateSegmentFactorySegmentStartIndex(List<Segment> segments) {
    int latestSegmentKey = Integer.parseInt(segments.get(0).getSegmentFileKey());
    segmentFactory.setSegmentStartIndex(++latestSegmentKey);
  }
}
