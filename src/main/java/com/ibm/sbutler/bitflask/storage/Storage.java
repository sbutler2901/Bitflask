package com.ibm.sbutler.bitflask.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages persisting and retrieving data
 */
public class Storage {

  private static final String WRITE_ERR_BAD_KEY = "Error writing data, provided key was null or empty";
  private static final String WRITE_ERR_BAD_VALUE = "Error writing data, provided value was null or empty";
  private static final String READ_ERR_BAD_KEY = "Error reading data, provided key was null or empty";

  private final List<StorageSegment> segmentFilesList = new ArrayList<>();

  public Storage() throws FileNotFoundException {
    createNewSegmentFile();
  }

  public Storage(StorageSegment storageSegment) {
    segmentFilesList.add(storageSegment.getSegmentIndex(), storageSegment);
  }

  /**
   * Gets the file index for the active segment
   *
   * @return the file index
   */
  public int getActiveSegmentFileIndex() {
    return segmentFilesList.size() - 1;
  }

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IOException              when seeking to the provided offset or writing fails
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  public void write(String key, String value) throws IOException, IllegalArgumentException {
    if (key == null || key.length() <= 0) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_KEY);
    } else if (value == null || value.length() <= 0) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_VALUE);
    }

    StorageSegment activeStorageSegment = getActiveStorageSegment();
    activeStorageSegment.write(key, value);

    if (activeStorageSegment.exceedsStorageThreshold()) {
      createNewSegmentFile();
    }
  }

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   * @throws IOException              when reading the key fails
   * @throws IllegalArgumentException when the provided key is invalid
   */
  public Optional<String> read(String key) throws IOException, IllegalArgumentException {
    if (key == null || key.length() <= 0) {
      throw new IllegalArgumentException(READ_ERR_BAD_KEY);
    }

    Optional<StorageSegment> optionalStorageSegment = findLatestSegmentWithKey(key);
    if (!optionalStorageSegment.isPresent()) {
      return Optional.empty();
    }

    return optionalStorageSegment.get().read(key);
  }

  /**
   * Creates a new segment file for storing data
   *
   * @throws FileNotFoundException when there is an error creating the new segment file
   */
  private void createNewSegmentFile() throws FileNotFoundException {
    StorageSegment newStorageSegment = new StorageSegment();
    segmentFilesList.add(newStorageSegment.getSegmentIndex(), newStorageSegment);
  }

  /**
   * Gets the current active storageSegment
   *
   * @return the active segment
   */
  private StorageSegment getActiveStorageSegment() {
    return segmentFilesList.get(getActiveSegmentFileIndex());
  }

  /**
   * Attempts to find a key in the list of storage segments starting with the most recently created
   *
   * @param key the key to be found
   * @return the found storage segment, if one exists
   */
  private Optional<StorageSegment> findLatestSegmentWithKey(String key) {
    for (int i = segmentFilesList.size() - 1; i >= 0; i--) {
      StorageSegment storageSegment = segmentFilesList.get(i);
      if (storageSegment.hasKey(key)) {
        return Optional.of(storageSegment);
      }
    }
    return Optional.empty();
  }
}
