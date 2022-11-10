package dev.sbutler.bitflask.storage.segment;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.segment.Encoder.Header;
import dev.sbutler.bitflask.storage.segment.Encoder.Offsets;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Represents a single self-contained file for storing data
 */
public final class Segment implements WritableSegment {

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

  public Optional<String> read(String key) throws IOException {
    if (!containsKey(key)) {
      return Optional.empty();
    }
    if (!isOpen()) {
      // TODO: create custom exception
      throw new IllegalStateException("This segment has been closed and cannot be read from");
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

  public void write(String key, String value) throws IOException {
    if (!isOpen()) {
      // TODO: create custom exception
      throw new IllegalStateException("This segment has been closed and cannot be written to");
    }

    byte[] encodedBytes = Encoder.encode(Header.KEY_VALUE, key, value);
    writeAndUpdateEntries(key, encodedBytes, Header.KEY_VALUE);
  }

  public void delete(String key) throws IOException {
    Entry entry = keyedEntryMap.get(key);
    if (entry.header().equals(Header.DELETED)) {
      return;
    }
    if (!isOpen()) {
      // TODO: create custom exception
      throw new IllegalStateException("This segment has been closed and cannot be written to");
    }

    byte[] encodedBytes = Encoder.encodeNoValue(Header.DELETED, key);
    writeAndUpdateEntries(key, encodedBytes, Header.DELETED);
  }

  /**
   * This operation will atomically write the encoded bytes and update the Segment's state.
   *
   * <p>A failure while writing will leave the Segment in the state it was prior to calling.
   */
  private void writeAndUpdateEntries(String key, byte[] encodedBytes, Header header)
      throws IOException {
    readWriteLock.writeLock().lock();
    try {
      long writeOffset = currentFileWriteOffset.get();
      Entry entry = new Entry(header, writeOffset);

      segmentFile.write(encodedBytes, writeOffset);
      currentFileWriteOffset.addAndGet(encodedBytes.length);

      keyedEntryMap.put(key, entry);
    } finally {
      readWriteLock.writeLock().unlock();
    }

    if (exceedsStorageThreshold() && sizeLimitExceededConsumer != null) {
      sizeLimitExceededConsumer.accept(this);
    }
  }

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > segmentSizeLimit;
  }

  public boolean containsKey(String key) {
    return keyedEntryMap.containsKey(key);
  }

  public int getSegmentFileKey() {
    return segmentFile.getSegmentFileKey();
  }

  public void close() {
    try {
      readWriteLock.writeLock().lock();
      segmentFile.close();
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public boolean isOpen() {
    return segmentFile.isOpen();
  }

  /**
   * Returns all keys mapped to their {@link Header}
   */
  ImmutableMap<String, Header> getSegmentKeyHeaderMap() {
    return keyedEntryMap.entrySet().stream()
        .collect(toImmutableMap(Map.Entry::getKey, (e) -> e.getValue().header()));
  }

  /**
   * Deletes this segment from the filesystem
   *
   * @throws IOException if there is an issue deleting the segment
   */
  void deleteSegment() throws IOException {
    if (isOpen()) {
      throw new IllegalStateException("Segment should be closed before deleting");
    }
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
  void markCompacted() {
    hasBeenCompacted = true;
  }

  /**
   * Checks if a segment has been marked as compacted
   *
   * @return whether the segment has been compacted or not
   */
  boolean hasBeenCompacted() {
    return hasBeenCompacted;
  }

  /**
   * Registers a consumer to be called once this segment's size limit has been reached.
   */
  void registerSizeLimitExceededConsumer(Consumer<Segment> sizeLimitExceededConsumer) {
    this.sizeLimitExceededConsumer = sizeLimitExceededConsumer;
  }

  @Override
  public String toString() {
    return "segment-" + getSegmentFileKey();
  }
}
