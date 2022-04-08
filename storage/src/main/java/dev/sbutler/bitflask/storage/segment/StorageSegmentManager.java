package dev.sbutler.bitflask.storage.segment;

import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.StorageExecutorService;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

public class StorageSegmentManager {

  private final ExecutorService executorService;
  private final Deque<StorageSegment> segmentFilesDeque = new ConcurrentLinkedDeque<>();

  @Inject
  StorageSegmentManager(@StorageExecutorService ExecutorService executorService)
      throws IOException {
    this.executorService = executorService;
    initializeSegments();
  }

  private void initializeSegments() throws IOException {
    // todo: check for previous segments
    createNewStorageSegment();
  }

  public StorageSegment getActiveSegment() throws IOException {
    checkAndCreateNewStorageSegment();
    return segmentFilesDeque.getFirst();
  }

  public Iterator<StorageSegment> getStorageSegmentsIterator() {
    Deque<StorageSegment> copiedDeque = new ArrayDeque<>(segmentFilesDeque);
    return copiedDeque.iterator();
  }

  private void createNewStorageSegment() throws IOException {
    StorageSegmentFile storageSegmentFile = new StorageSegmentFile(executorService);
    StorageSegment newStorageSegment = new StorageSegment(storageSegmentFile);
    segmentFilesDeque.offerFirst(newStorageSegment);
  }

  private synchronized void checkAndCreateNewStorageSegment() throws IOException {
    StorageSegment currentActiveStorageSegment = segmentFilesDeque.getFirst();
    if (currentActiveStorageSegment.exceedsStorageThreshold()) {
      createNewStorageSegment();
    }
  }

}
