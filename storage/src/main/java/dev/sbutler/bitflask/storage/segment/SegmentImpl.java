package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

class SegmentImpl implements Segment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private static final String DELIMITER = ";";
  private static final String ENCODING_FORMAT = "%s" + DELIMITER + "%s" + DELIMITER;

  private final SegmentFile segmentFile;
  private final ConcurrentMap<String, Entry> keyEntryMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong(0);

  public SegmentImpl(SegmentFile segmentFile) {
    this.segmentFile = segmentFile;
  }

  @Override
  public void write(String key, String value) {
    byte[] encodedKeyAndValue = encodeKeyAndValue(key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedKeyAndValue.length);

    try {
      segmentFile.write(encodedKeyAndValue, writeOffset);
      createAndAddNewEntry(key, value, writeOffset);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static byte[] encodeKeyAndValue(String key, String value) {
    String encoded = String.format(ENCODING_FORMAT, key, value);
    return encoded.getBytes(StandardCharsets.UTF_8);
  }

  private void createAndAddNewEntry(String key, String value, long offset) {
    Entry entry = new EntryImpl(offset, key.length(), value.length());
    // Handle newer value being written and added in another thread for same key
    keyEntryMap.merge(key, entry, (retrievedEntry, writtenEntry) ->
        retrievedEntry.getSegmentFileOffset() < writtenEntry.getSegmentFileOffset()
            ? writtenEntry
            : retrievedEntry
    );
  }

  @Override
  public Optional<String> read(String key) {
    if (!containsKey(key)) {
      return Optional.empty();
    }

    Entry entry = keyEntryMap.get(key);
    try {
      byte[] readBytes = segmentFile.read(entry.getTotalLength(),
          entry.getSegmentFileOffset());
      String value = decodeValue(readBytes, entry.getKeyLength());
      return Optional.of(value);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  static String decodeValue(byte[] readBytes, int keyLength) {
    String entry = new String(readBytes).trim();
    return entry.substring(keyLength + 1, readBytes.length - 1);
  }

  @Override
  public boolean containsKey(String key) {
    return keyEntryMap.containsKey(key);
  }

  @Override
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > NEW_SEGMENT_THRESHOLD;
  }

  record EntryImpl(long segmentFileOffset, int keyLength, int valueLength) implements Entry {

    private static final String INVALID_ARGS = "Invalid entry values: %d, %d, %d";

    EntryImpl {
      if (segmentFileOffset < 0 || keyLength <= 0 || valueLength <= 0) {
        throw new IllegalArgumentException(
            String.format(INVALID_ARGS, segmentFileOffset, keyLength, valueLength));
      }
    }

    @Override
    public long getSegmentFileOffset() {
      return segmentFileOffset;
    }

    @Override
    public int getKeyLength() {
      return keyLength;
    }

    @Override
    public int getValueLength() {
      return valueLength;
    }

    @Override
    public int getTotalLength() {
      return keyLength + valueLength;
    }

    @Override
    public String toString() {
      return "Entry{" +
          "segmentOffset=" + segmentFileOffset +
          ", keyLength=" + keyLength +
          ", valueLength=" + valueLength +
          '}';
    }
  }
}
