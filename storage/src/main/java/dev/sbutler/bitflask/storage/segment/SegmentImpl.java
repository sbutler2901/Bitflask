package dev.sbutler.bitflask.storage.segment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class SegmentImpl implements Segment {

  private final SegmentFile segmentFile;
  private final ConcurrentMap<String, Long> keyedEntryFileOffsetMap;
  private final AtomicLong currentFileWriteOffset;
  private final long segmentSizeLimit;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private volatile boolean hasBeenCompacted = false;

  public SegmentImpl(SegmentFile segmentFile, ConcurrentMap<String, Long> keyedEntryFileOffsetMap,
      AtomicLong currentFileWriteOffset, long segmentSizeLimit) {
    this.segmentFile = segmentFile;
    this.keyedEntryFileOffsetMap = keyedEntryFileOffsetMap;
    this.currentFileWriteOffset = currentFileWriteOffset;
    this.segmentSizeLimit = segmentSizeLimit;
  }

  @Override
  public void write(String key, String value) throws IOException {
    if (!isOpen()) {
      throw new RuntimeException("This segment has been closed and cannot be written to");
    }

    byte[] encodedKeyAndValue = encodeKeyAndValue(key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedKeyAndValue.length);

    try {
      readWriteLock.writeLock().lock();
      segmentFile.write(encodedKeyAndValue, writeOffset);
    } finally {
      readWriteLock.writeLock().unlock();
    }

    keyedEntryFileOffsetMap.merge(key, writeOffset, (retrievedOffset, writtenOffset) ->
        retrievedOffset < writtenOffset
            ? writtenOffset
            : retrievedOffset
    );
  }

  @Override
  public Optional<String> read(String key) throws IOException {
    if (!isOpen()) {
      throw new RuntimeException("This segment has been closed and cannot be read from");
    }
    if (!containsKey(key)) {
      return Optional.empty();
    }

    long entryFileOffset = keyedEntryFileOffsetMap.get(key);
    long valueLengthOffsetStart = entryFileOffset + 1 + key.length();
    long valueOffsetStart = valueLengthOffsetStart + 1;

    try {
      readWriteLock.readLock().lock();
      int valueLength = segmentFile.readByte(valueLengthOffsetStart);
      String value = segmentFile.readAsString(valueLength, valueOffsetStart);
      return Optional.of(value);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * Encodes a key and value into a byte array. Uses a single byte each for encoding key and value
   * lengths (limiting them to a max length of 256)
   *
   * @param key   the key to be encoded
   * @param value the value to be encoded
   * @return the combined encoding of the key and value
   */
  static byte[] encodeKeyAndValue(String key, String value) {
    verifyEncodedArgs(key, value);

    char encodedKeyLength = (char) key.length();
    char encodedValueLength = (char) value.length();
    String encoded = String.format("%c%s%c%s", encodedKeyLength, key, encodedValueLength,
        value);
    return encoded.getBytes(StandardCharsets.UTF_8);
  }

  private static void verifyEncodedArgs(String key, String value) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
    checkNotNull(value);
    checkArgument(!value.isBlank(), "Expected non-blank key, but was [%s]", value);
    checkArgument(value.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        value.length());
  }

  @Override
  public boolean containsKey(String key) {
    return keyedEntryFileOffsetMap.containsKey(key);
  }

  @Override
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > segmentSizeLimit;
  }

  @Override
  public ImmutableSet<String> getSegmentKeys() {
    return ImmutableSet.copyOf(keyedEntryFileOffsetMap.keySet());
  }

  @Override
  public int getSegmentFileKey() {
    return segmentFile.getSegmentFileKey();
  }

  @Override
  public void close() {
    try {
      readWriteLock.writeLock().lock();
      segmentFile.close();
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public boolean isOpen() {
    return segmentFile.isOpen();
  }

  @Override
  public void delete() throws IOException {
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

  @Override
  public void markCompacted() {
    hasBeenCompacted = true;
  }

  @Override
  public boolean hasBeenCompacted() {
    return hasBeenCompacted;
  }

  @Override
  public String toString() {
    return "segment-" + getSegmentFileKey();
  }
}
