package dev.sbutler.bitflask.storage.segment;

import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.StorageExecutorService;
import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

public class SegmentManagerImpl implements SegmentManager {

  private final ExecutorService executorService;
  private final Deque<Segment> segmentFilesDeque = new ConcurrentLinkedDeque<>();

  @Inject
  SegmentManagerImpl(@StorageExecutorService ExecutorService executorService)
      throws IOException {
    this.executorService = executorService;
    initializeSegments();
  }

  private void initializeSegments() throws IOException {
    // todo: check for previous segments
    createNewSegment();
  }

  @Override
  public Segment getActiveSegment() throws IOException {
    checkAndCreateNewSegment();
    return segmentFilesDeque.getFirst();
  }

  @Override
  public Iterator<Segment> getSegmentsIterator() {
    return Collections.unmodifiableCollection(segmentFilesDeque).iterator();
  }

  private synchronized void createNewSegment() throws IOException {
    SegmentFile segmentFile = new SegmentFile(executorService);
    Segment newSegment = new SegmentImpl(segmentFile);
    segmentFilesDeque.offerFirst(newSegment);
  }

  /**
   * Checks if the active segment has exceeded its threshold and creates a new one if so. This
   * method blocks other writes while a new segment is being created
   *
   * @throws IOException if there is an issue creating a new segment file
   */
  private synchronized void checkAndCreateNewSegment() throws IOException {
    Segment currentActiveSegment = segmentFilesDeque.getFirst();
    if (currentActiveSegment.exceedsStorageThreshold()) {
      createNewSegment();
    }
  }

}
