package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableSet;
import dev.sbutler.bitflask.storage.segment.Encoder.Header;
import dev.sbutler.bitflask.storage.segment.Encoder.Offsets;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Represents a single self-contained file for storing data
 */
public final class Segment {

  record Entry(Header header, long offset) {

  }

  private final SegmentFile segmentFile;
  private final ConcurrentMap<String, Entry> keyedEntryMap;
  private final AtomicLong currentFileWriteOffset;
  private final long segmentSizeLimit;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private volatile Consumer<Segment> sizeLimitExceededConsumer = null;
  private volatile boolean hasBeenCompacted = false;

  Segment(SegmentFile segmentFile, ConcurrentMap<String, Entry> keyedEntryMap,
      AtomicLong currentFileWriteOffset, long segmentSizeLimit) {
    this.segmentFile = segmentFile;
    this.keyedEntryMap = keyedEntryMap;
    this.currentFileWriteOffset = currentFileWriteOffset;
    this.segmentSizeLimit = segmentSizeLimit;
  }

  /**
   * Reads the provided key's value from the segment file
   *
   * @param key the key to find the data in the segment file
   * @return the value for the key from the segment file, if it exists
   */
  public Optional<String> read(String key) throws IOException {
    if (!containsKey(key)) {
      return Optional.empty();
    }
    if (!isOpen()) {
      throw new RuntimeException("This segment has been closed and cannot be read from");
    }

    Entry entry = keyedEntryMap.get(key);
    if (entry.header().equals(Header.DELETED)) {
      return Optional.empty();
    }

    Offsets offsets = Encoder.decode(entry.offset(), key.length());
    readWriteLock.readLock().lock();
    try {
      int valueLength = segmentFile.readByte(offsets.valueLength());
      String value = segmentFile.readAsString(valueLength, offsets.value());
      return Optional.of(value);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   */
  public void write(String key, String value) throws IOException {
    if (!isOpen()) {
      // TODO: adjust for consumers to try again
      throw new RuntimeException("This segment has been closed and cannot be written to");
    }

    byte[] encodedBytes = Encoder.encode(Header.KEY_VALUE, key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedBytes.length);
    Entry entry = new Entry(Header.KEY_VALUE, writeOffset);

    writeAndUpdateEntries(key, encodedBytes, entry);
  }

  public void delete(String key) throws IOException {
    Entry entry = keyedEntryMap.get(key);
    if (entry.header().equals(Header.DELETED)) {
      return;
    }
    if (!isOpen()) {
      // TODO: adjust for consumers to try again
      throw new RuntimeException("This segment has been closed and cannot be written to");
    }

    byte[] encodedBytes = Encoder.encodeNoValue(Header.DELETED, key);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedBytes.length);
    Entry tombstoneEntry = new Entry(Header.DELETED, writeOffset);

    writeAndUpdateEntries(key, encodedBytes, tombstoneEntry);
  }

  private void writeAndUpdateEntries(String key, byte[] encodedBytes, Entry entry)
      throws IOException {
    readWriteLock.writeLock().lock();
    try {
      segmentFile.write(encodedBytes, entry.offset());
      keyedEntryMap.put(key, entry);
    } finally {
      readWriteLock.writeLock().unlock();
    }

    if (exceedsStorageThreshold() && sizeLimitExceededConsumer != null) {
      sizeLimitExceededConsumer.accept(this);
    }
  }

  /**
   * Checks if the segment contains the provided key
   *
   * @param key the key to be searched for
   * @return whether it contains the key, or not
   */
  public boolean containsKey(String key) {
    return keyedEntryMap.containsKey(key);
  }

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > segmentSizeLimit;
  }

  /**
   * Returns all keys stored by the segment
   *
   * @return a set of the keys stored by the segment
   */
  public ImmutableSet<String> getSegmentKeys() {
    return ImmutableSet.copyOf(keyedEntryMap.keySet());
  }

  /**
   * Returns the segment's file's key
   *
   * @return the segment's file's key
   */
  public int getSegmentFileKey() {
    return segmentFile.getSegmentFileKey();
  }

  /**
   * Closes the segment for reading and writing
   */
  public void close() {
    try {
      readWriteLock.writeLock().lock();
      segmentFile.close();
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * Checks if the segment is open and able to be read or written
   *
   * @return whether the segment is open or not
   */
  public boolean isOpen() {
    return segmentFile.isOpen();
  }

  /**
   * Deletes this segment from the filesystem
   *
   * @throws IOException if there is an issue deleting the segment
   */
  public void deleteSegment() throws IOException {
    if (isOpen()) {
      throw new RuntimeException("Segment should be closed before deleting");
    }
    // todo: tombstone segment's file in case of failure
    try {
      readWriteLock.writeLock().lock();
      Files.delete(segmentFile.getSegmentFilePath());
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * Marks the segment as compacted
   */
  public void markCompacted() {
    hasBeenCompacted = true;
  }

  /**
   * Checks if a segment has been marked as compacted
   *
   * @return whether the segment has been compacted or not
   */
  public boolean hasBeenCompacted() {
    return hasBeenCompacted;
  }

  /**
   * Registers a consumer to be called once this segment's size limit has been reached.
   */
  public void registerSizeLimitExceededConsumer(Consumer<Segment> sizeLimitExceededConsumer) {
    this.sizeLimitExceededConsumer = sizeLimitExceededConsumer;
  }

  @Override
  public String toString() {
    return "segment-" + getSegmentFileKey();
  }
}
