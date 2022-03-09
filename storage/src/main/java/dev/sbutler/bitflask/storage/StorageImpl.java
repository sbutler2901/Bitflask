package dev.sbutler.bitflask.storage;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages persisting and retrieving data
 */
class StorageImpl implements Storage {

  private static final String WRITE_ERR_BAD_KEY = "Error writing data, provided key was null or empty";
  private static final String WRITE_ERR_BAD_VALUE = "Error writing data, provided value was null or empty";
  private static final String READ_ERR_BAD_KEY = "Error reading data, provided key was null or empty";

  private final AtomicInteger activeStorageSegmentIndex = new AtomicInteger(0);

  private final ThreadPoolExecutor threadPool;
  private final List<StorageSegment> segmentFilesList = new CopyOnWriteArrayList<>();

  /**
   * Creates a new storage instance for managing getting and setting key-value pairs
   *
   * @param threadPool the thread pool executor service for which this storage instance will execute
   *                   operations
   * @throws IOException when creating the initial storage file fails
   */
  public StorageImpl(ThreadPoolExecutor threadPool) throws IOException {
    this.threadPool = threadPool;
    createInitialStorageSegment();
  }

  private void createInitialStorageSegment() throws IOException {
    StorageSegment newStorageSegment = new StorageSegment(threadPool,
        activeStorageSegmentIndex.get());
    segmentFilesList.add(activeStorageSegmentIndex.get(), newStorageSegment);
  }

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IOException              when creating a new segment file fails
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  public void write(String key, String value) throws IOException, IllegalArgumentException {
    if (key == null || key.length() <= 0) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_KEY);
    } else if (value == null || value.length() <= 0) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_VALUE);
    }

    int activeIndex = activeStorageSegmentIndex.get();
    StorageSegment activeStorageSegment = segmentFilesList.get(activeIndex);
    activeStorageSegment.write(key, value);

    checkAndCreateNewStorageSegment();
  }

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  public Optional<String> read(String key) {
    if (key == null || key.length() <= 0) {
      throw new IllegalArgumentException(READ_ERR_BAD_KEY);
    }

    Optional<StorageSegment> optionalStorageSegment = findLatestSegmentWithKey(key);
    if (optionalStorageSegment.isEmpty()) {
      return Optional.empty();
    }

    return optionalStorageSegment.get().read(key);
  }

  /**
   * Checks if a new storage segment should be created and creates it if needed
   *
   * @throws IOException when there is an error creating the new segment file
   */
  private synchronized void checkAndCreateNewStorageSegment() throws IOException {
    int activeIndex = activeStorageSegmentIndex.get();
    StorageSegment activeStorageSegment = segmentFilesList.get(activeIndex);

    if (activeStorageSegment.exceedsStorageThreshold()) {
      int newSegmentIndex = activeStorageSegmentIndex.incrementAndGet();
      StorageSegment newStorageSegment = new StorageSegment(threadPool, newSegmentIndex);
      segmentFilesList.add(newSegmentIndex, newStorageSegment);
    }
  }

  /**
   * Attempts to find a key in the list of storage segments starting with the most recently created
   *
   * @param key the key to be found
   * @return the found storage segment, if one exists
   */
  private Optional<StorageSegment> findLatestSegmentWithKey(String key) {
    int lastIndex = segmentFilesList.size();
    ListIterator<StorageSegment> segmentListIterator = segmentFilesList.listIterator(lastIndex);
    while (segmentListIterator.hasPrevious()) {
      StorageSegment storageSegment = segmentListIterator.previous();
      if (storageSegment.containsKey(key)) {
        return Optional.of(storageSegment);
      }
    }
    return Optional.empty();
  }
}
