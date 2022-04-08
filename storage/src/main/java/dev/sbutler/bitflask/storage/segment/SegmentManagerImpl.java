package dev.sbutler.bitflask.storage.segment;

import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.StorageExecutorService;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

public class SegmentManagerImpl {

  private final ExecutorService executorService;
  private final Deque<SegmentImpl> segmentFilesDeque = new ConcurrentLinkedDeque<>();

  @Inject
  SegmentManagerImpl(@StorageExecutorService ExecutorService executorService)
      throws IOException {
    this.executorService = executorService;
    initializeSegments();
  }

  private void initializeSegments() throws IOException {
    // todo: check for previous segments
    createNewStorageSegment();
  }

  public SegmentImpl getActiveSegment() throws IOException {
    checkAndCreateNewStorageSegment();
    return segmentFilesDeque.getFirst();
  }

  public Iterator<SegmentImpl> getStorageSegmentsIterator() {
    Deque<SegmentImpl> copiedDeque = new ArrayDeque<>(segmentFilesDeque);
    return copiedDeque.iterator();
  }

  private void createNewStorageSegment() throws IOException {
    SegmentFile segmentFile = new SegmentFile(executorService);
    SegmentImpl newSegment = new SegmentImpl(segmentFile);
    segmentFilesDeque.offerFirst(newSegment);
  }

  private synchronized void checkAndCreateNewStorageSegment() throws IOException {
    SegmentImpl currentActiveSegment = segmentFilesDeque.getFirst();
    if (currentActiveSegment.exceedsStorageThreshold()) {
      createNewStorageSegment();
    }
  }

}
