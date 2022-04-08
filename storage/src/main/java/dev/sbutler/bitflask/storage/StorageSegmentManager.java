package dev.sbutler.bitflask.storage;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

class StorageSegmentManager {

  private final ExecutorService executorService;
  private final AtomicInteger activeStorageSegmentIndex = new AtomicInteger(-1);
  private final List<StorageSegment> segmentFilesList = new CopyOnWriteArrayList<>();

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
    int activeIndex = activeStorageSegmentIndex.get();
    return segmentFilesList.get(activeIndex);
  }

  ListIterator<StorageSegment> getStorageSegmentsIteratorReversed() {
    int lastIndex = segmentFilesList.size();
    return segmentFilesList.listIterator(lastIndex);
  }

  private synchronized void createNewStorageSegment() throws IOException {
    int newStorageSegmentIndex = activeStorageSegmentIndex.incrementAndGet();
    StorageSegmentFile storageSegmentFile = new StorageSegmentFile(executorService,
        newStorageSegmentIndex);
    StorageSegment newStorageSegment = new StorageSegment(storageSegmentFile);
    segmentFilesList.add(newStorageSegmentIndex, newStorageSegment);
  }

  private synchronized void checkAndCreateNewStorageSegment() throws IOException {
    int currentActiveStorageSegmentIndex = activeStorageSegmentIndex.get();
    StorageSegment currentActiveStorageSegment = segmentFilesList.get(
        currentActiveStorageSegmentIndex);
    if (currentActiveStorageSegment.exceedsStorageThreshold()) {
      createNewStorageSegment();
    }
  }

}
