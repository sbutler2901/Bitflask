package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

class SegmentImpl implements Segment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private static final char DELIMITER = ';';
  private static final String ENCODING_FORMAT = "%c%s%c%s";

  private final SegmentFile segmentFile;
  private final ConcurrentMap<String, Entry> keyEntryMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Long> keyedEntryFileOffsetMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong();

  public SegmentImpl(SegmentFile segmentFile) throws IOException {
    this.segmentFile = segmentFile;

    currentFileWriteOffset.set(segmentFile.size());
    if (currentFileWriteOffset.get() > 0) {
      loadFileEntries();
    }
  }

  private void loadFileEntries() throws IOException {
    byte[] loadedFile = segmentFile.read((int) currentFileWriteOffset.get(), 0);

    StringBuilder keyBuilder = new StringBuilder();
    StringBuilder valueBuilder = new StringBuilder();
    boolean keyCompleted = false;
    long nextEntryOffsetStart = 0;
    for (int i = 0; i < loadedFile.length; i++) {
      char nextChar = (char) loadedFile[i];
      if (nextChar == DELIMITER) {
        if (keyCompleted) {
          createAndAddNewEntry(keyBuilder.toString(), valueBuilder.toString(),
              nextEntryOffsetStart);

          // reset for next entry
          keyBuilder.setLength(0);
          valueBuilder.setLength(0);
          nextEntryOffsetStart = i + 1;
          keyCompleted = false;
        } else {
          keyCompleted = true;
        }
      } else {
        if (keyCompleted) {
          valueBuilder.append(nextChar);
        } else {
          keyBuilder.append(nextChar);
        }
      }
    }
  }

  @Override
  public void write(String key, String value) {
    byte[] encodedKeyAndValue = encodeKeyAndValue(key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedKeyAndValue.length);

    try {
      segmentFile.write(encodedKeyAndValue, writeOffset);
      keyedEntryFileOffsetMap.merge(key, writeOffset, (retrievedOffset, writtenOffset) ->
          retrievedOffset < writtenOffset
              ? writtenOffset
              : retrievedOffset
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
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

    long entryFileOffset = keyedEntryFileOffsetMap.get(key);
    try {
      long valueLengthOffsetStart = entryFileOffset + 1 + key.length();
      int valueLength = segmentFile.readByte(valueLengthOffsetStart);
      byte[] valueBytes = segmentFile.read(valueLength, valueLengthOffsetStart + 1);
      String value = new String(valueBytes);
      return Optional.of(value);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  static byte[] encodeKeyAndValue(String key, String value) {
    char encodedKeyLength = (char) key.length();
    char encodedValueLength = (char) value.length();
    String encoded = String.format(ENCODING_FORMAT, encodedKeyLength, key, encodedValueLength,
        value);
    return encoded.getBytes(StandardCharsets.UTF_8);
  }

  static String decodeValue(byte[] readBytes, int keyLength) {
    String entry = new String(readBytes).trim();
    return entry.substring(keyLength + 1, readBytes.length - 1);
  }

  @Override
  public boolean containsKey(String key) {
    return keyedEntryFileOffsetMap.containsKey(key);
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
      // Include delimiters for entry
      return 2 + keyLength + valueLength;
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
