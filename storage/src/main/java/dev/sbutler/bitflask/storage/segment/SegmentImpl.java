package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

class SegmentImpl implements Segment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private static final String ENCODING_FORMAT = "%c%s%c%s";

  private final SegmentFile segmentFile;
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
    long nextOffsetStart = 0;
    while (nextOffsetStart < currentFileWriteOffset.get()) {
      long entryStartOffset = nextOffsetStart;
      int keyLength = segmentFile.readByte(nextOffsetStart++);
      byte[] keyBytes = segmentFile.read(keyLength, nextOffsetStart);
      String key = new String(keyBytes);
      keyedEntryFileOffsetMap.put(key, entryStartOffset);

      nextOffsetStart += keyLength;
      int valueLength = segmentFile.readByte(nextOffsetStart++);
      nextOffsetStart += valueLength;
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

  @Override
  public Optional<String> read(String key) {
    if (!containsKey(key)) {
      return Optional.empty();
    }

    long entryFileOffset = keyedEntryFileOffsetMap.get(key);
    long valueLengthOffsetStart = entryFileOffset + 1 + key.length();
    try {
      int valueLength = segmentFile.readByte(valueLengthOffsetStart);
      long valueOffsetStart = valueLengthOffsetStart + 1;
      byte[] valueBytes = segmentFile.read(valueLength, valueOffsetStart);
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

}
