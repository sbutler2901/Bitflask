package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class SegmentImpl implements Segment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private final SegmentFile segmentFile;
  private final ConcurrentMap<String, Long> keyedEntryFileOffsetMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private volatile boolean isFrozen = false;
  private volatile boolean hasBeenCompacted = false;
  private volatile boolean isClosed = false;

  public SegmentImpl(SegmentFile segmentFile) throws IOException {
    this.segmentFile = segmentFile;

    currentFileWriteOffset.set(segmentFile.size());
    if (currentFileWriteOffset.get() > 0) {
      loadFileEntries();
    }
  }

  // todo: handle empty spaces that occur because of failed writes
  private void loadFileEntries() throws IOException {
    long nextOffsetStart = 0;
    while (nextOffsetStart < currentFileWriteOffset.get()) {
      long entryStartOffset = nextOffsetStart;

      // Get key and update offset map
      int keyLength = segmentFile.readByte(nextOffsetStart++);
      String key = segmentFile.readAsString(keyLength, nextOffsetStart);
      nextOffsetStart += keyLength;
      keyedEntryFileOffsetMap.put(key, entryStartOffset);

      // Start next iteration offset after current entry's value
      int valueLength = segmentFile.readByte(nextOffsetStart++);
      nextOffsetStart += valueLength;
    }
  }

  @Override
  public void write(String key, String value) throws IOException {
    if (isClosed) {
      throw new RuntimeException("This segment has been closed and cannot be written to");
    } else if (isFrozen) {
      throw new RuntimeException("This segment has been frozen and cannot be written to");
    }

    byte[] encodedKeyAndValue = encodeKeyAndValue(key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedKeyAndValue.length);

    readWriteLock.writeLock().lock();
    try {
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
    if (isClosed) {
      throw new RuntimeException("This segment has been closed and cannot be read from");
    }
    if (!containsKey(key)) {
      return Optional.empty();
    }

    long entryFileOffset = keyedEntryFileOffsetMap.get(key);
    long valueLengthOffsetStart = entryFileOffset + 1 + key.length();
    long valueOffsetStart = valueLengthOffsetStart + 1;

    readWriteLock.readLock().lock();
    try {
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
    if (key.length() > 256) {
      throw new IllegalArgumentException("A key longer than 256 chars cannot be encoded");
    } else if (value.length() > 256) {
      throw new IllegalArgumentException("A value longer than 256 chars cannot be encoded");
    }
  }

  @Override
  public boolean containsKey(String key) {
    return keyedEntryFileOffsetMap.containsKey(key);
  }

  @Override
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > NEW_SEGMENT_THRESHOLD;
  }

  @Override
  public Set<String> getSegmentKeys() {
    return keyedEntryFileOffsetMap.keySet();
  }

  @Override
  public int getSegmentFileKey() {
    return segmentFile.getSegmentFileKey();
  }

  @Override
  public void closeAndDelete() throws IOException {
    readWriteLock.writeLock().lock();
    isClosed = true;
    try {
      segmentFile.close();
      Files.delete(segmentFile.getSegmentFilePath());
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void markFrozen() {
    isFrozen = true;
  }

  @Override
  public boolean isFrozen() {
    return isFrozen;
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
  public boolean isClosed() {
    return isClosed;
  }

}
