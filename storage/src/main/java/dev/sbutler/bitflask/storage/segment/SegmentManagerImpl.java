package dev.sbutler.bitflask.storage.segment;

import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.StorageExecutorService;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

class SegmentManagerImpl implements SegmentManager {

  private static final String DEFAULT_SEGMENT_FILE_PATH = "store/segment_%d.txt";
  private static final StandardOpenOption[] fileOptions = {
      StandardOpenOption.CREATE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE,
//      StandardOpenOption.TRUNCATE_EXISTING
  };
  private static final Set<StandardOpenOption> fileChannelOptions = new HashSet<>(
      Arrays.asList(fileOptions));

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
  public synchronized Segment getActiveSegment() throws IOException {
    Segment currentActiveSegment = segmentFilesDeque.getFirst();
    if (currentActiveSegment.exceedsStorageThreshold()) {
      return createAndAddNextActiveSegment();
    }
    return currentActiveSegment;
  }

  @Override
  public Iterator<Segment> getSegmentsIterator() {
    return Collections.unmodifiableCollection(segmentFilesDeque).iterator();
  }

  private synchronized Segment createAndAddNextActiveSegment() throws IOException {
    AsynchronousFileChannel segmentFileChannel = getNextSegmentFileChannel();
    SegmentFile segmentFile = new SegmentFile(segmentFileChannel);
    Segment segment = new SegmentImpl(segmentFile);
    segmentFilesDeque.offerFirst(segment);
    return segment;
  }

  private AsynchronousFileChannel getNextSegmentFileChannel() throws IOException {
    Path newSegmentFilePath = getNextSegmentFilePath();
    return AsynchronousFileChannel
        .open(newSegmentFilePath, fileChannelOptions, executorService);
  }

  private Path getNextSegmentFilePath() {
    int segmentIndex = nextSegmentIndex.getAndIncrement();
    return Paths.get(String.format(DEFAULT_SEGMENT_FILE_PATH, segmentIndex));
  }

}
