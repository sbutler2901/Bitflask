package dev.sbutler.bitflask.storage.segment;

import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.StorageExecutorService;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD = 2;

  private static final String DEFAULT_SEGMENT_FILE_PATH = "store/segment_%d.txt";
  private static final StandardOpenOption[] fileOptions = {
      StandardOpenOption.CREATE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE,
//      StandardOpenOption.TRUNCATE_EXISTING
  };
  private static final Set<StandardOpenOption> fileChannelOptions = new HashSet<>(
      Arrays.asList(fileOptions));

  // todo: handle detecting previous segments after compaction
  private static final AtomicInteger nextSegmentIndex = new AtomicInteger(0);

  private final ExecutorService executorService;
  private final Deque<Segment> segmentFilesDeque = new ConcurrentLinkedDeque<>();

  @Inject
  SegmentManagerImpl(@StorageExecutorService ExecutorService executorService)
      throws IOException {
    this.executorService = executorService;
    initializeSegments();
  }

  private void initializeSegments() throws IOException {
    boolean loadNextSegment = true;
    while (loadNextSegment) {
      Segment nextSegment = createAndAddNextActiveSegment();
      loadNextSegment = nextSegment.exceedsStorageThreshold();
    }
  }

  @Override
  public synchronized void write(String key, String value) throws IOException {
    Segment activeSegment = getActiveSegment();
    activeSegment.write(key, value);
  }

  @Override
  public Optional<String> read(String key) {
    Optional<Segment> optionalSegment = findLatestSegmentWithKey(key);
    if (optionalSegment.isEmpty()) {
      return Optional.empty();
    }
    return optionalSegment.get().read(key);
  }

  /**
   * Attempts to find a key in the list of storage segments starting with the most recently created
   *
   * @param key the key to be found
   * @return the found storage segment, if one exists
   */
  private Optional<Segment> findLatestSegmentWithKey(String key) {
    for (Segment segment : segmentFilesDeque) {
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }

  public synchronized Segment getActiveSegment() throws IOException {
    Segment currentActiveSegment = segmentFilesDeque.getFirst();
    if (currentActiveSegment.exceedsStorageThreshold()) {
      return getNextActiveSegment();
    }
    return currentActiveSegment;
  }

  private synchronized Segment getNextActiveSegment() throws IOException {
    // blocks writing until new segment is created or compaction is completed
    if (shouldPerformCompaction()) {
      compactSegments();
    } else {
      createAndAddNextActiveSegment();
    }
    return segmentFilesDeque.getFirst();
  }

  private Segment createNewSegment() throws IOException {
    Path segmentPath = getNextSegmentFilePath();
    AsynchronousFileChannel segmentFileChannel = getNextSegmentFileChannel(segmentPath);
    SegmentFile segmentFile = new SegmentFile(segmentFileChannel, segmentPath);
    return new SegmentImpl(segmentFile);
  }

  private Segment createAndAddNextActiveSegment() throws IOException {
    Segment segment = createNewSegment();
    segmentFilesDeque.offerFirst(segment);
    return segment;
  }

  private AsynchronousFileChannel getNextSegmentFileChannel(Path nextSegmentFilePath)
      throws IOException {
    return AsynchronousFileChannel
        .open(nextSegmentFilePath, fileChannelOptions, executorService);
  }

  private Path getNextSegmentFilePath() {
    int segmentIndex = nextSegmentIndex.getAndIncrement();
    return Paths.get(String.format(DEFAULT_SEGMENT_FILE_PATH, segmentIndex));
  }

  private boolean shouldPerformCompaction() {
    return segmentFilesDeque.size() % DEFAULT_COMPACTION_THRESHOLD == 0;
  }

  private synchronized void compactSegments() throws IOException {
    Map<String, Segment> keySegmentMap = createKeySegmentMap();
    Segment compactedSegment = createCompactedSegment(keySegmentMap);
    deleteCompactedSegments();
    finalizeCompaction(compactedSegment);
  }

  private Map<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : segmentFilesDeque) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return keySegmentMap;
  }

  private Segment createCompactedSegment(Map<String, Segment> keySegmentMap) throws IOException {
    Segment compactedSegment = createNewSegment();

    for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
      String key = entry.getKey();
      Optional<String> valueOptional = entry.getValue().read(key);
      if (valueOptional.isEmpty()) {
        throw new RuntimeException("Compaction failure: value not found while reading segment");
      }
      compactedSegment.write(key, valueOptional.get());
    }

    return compactedSegment;
  }


  private void deleteCompactedSegments() {
    for (Segment segment : segmentFilesDeque) {
      try {
        segment.closeAndDelete();
      } catch (IOException e) {
        System.err.println("Failure to close segment: " + e.getMessage());
      }
    }
  }

  private void finalizeCompaction(Segment compactedSegment) {
    segmentFilesDeque.clear();
    segmentFilesDeque.offerFirst(compactedSegment);
  }

}
