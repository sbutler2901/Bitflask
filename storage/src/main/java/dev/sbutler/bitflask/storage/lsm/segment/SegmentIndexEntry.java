package dev.sbutler.bitflask.storage.lsm.segment;


import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A single key:offset entry in a segment index.
 *
 * @param key    the key for an associated Segment {@link Entry}. Cannot be empty or greater than
 *               {@link Entry#KEY_MAX_LENGTH}.
 * @param offset the offset of the associated Segment {@link Entry} in the Segment's file. Cannot be
 *               negative.
 */
record SegmentIndexEntry(String key, long offset) {

  /**
   * The minimum number of bits used to represent a SegmentIndexEntry.
   */
  static final int MIN_SIZE = UnsignedShort.SIZE + Long.SIZE + Byte.SIZE;

  /**
   * The minimum number of bytes to represent a SegmentIndexEntry.
   */
  static final int MIN_BYTES = MIN_SIZE / Byte.SIZE;

  SegmentIndexEntry {
    checkArgument(!key.isEmpty(), "Key must not be empty.");
    checkArgument(key.length() <= Entry.KEY_MAX_LENGTH,
        "Key length greater than allowed. Provided [%s], max allowed [%s]",
        key.length(),
        Entry.KEY_MAX_LENGTH);
    checkArgument(offset >= 0L, "Offset negative. Provided [%s]", offset);
  }

  /**
   * Creates a new SegmentIndexEntry from the provided byte array.
   *
   * <p>The first 2 indices will be interpreted as a 2-byte unsigned short representing the key
   * length. The next 8 indices will be interpreted as an 8-byte long representing the offset. The
   * derived key length will determine the amount of bytes for the key.
   */
  static SegmentIndexEntry fromBytes(byte[] bytes) {
    checkArgument(bytes.length >= MIN_BYTES,
        "Byte array length invalid. Provided [%s], expected at least [%s]",
        bytes.length, MIN_BYTES);

    PartialEntry partialEntry = PartialEntry.fromBytes(bytes);

    byte[] keyBytes = Arrays.copyOfRange(
        bytes,
        PartialEntry.BYTES,
        PartialEntry.BYTES + partialEntry.keyLength.value());

    return new SegmentIndexEntry(new String(keyBytes), partialEntry.offset());
  }

  byte[] getBytes() {
    PartialEntry partialEntry = new PartialEntry(
        new UnsignedShort(key.length()),
        offset);
    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

    return Bytes.concat(partialEntry.getBytes(), keyBytes);
  }

  record PartialEntry(UnsignedShort keyLength, long offset) {

    /**
     * The minimum number of bits used to represent a PartialEntry.
     */
    static final int SIZE = UnsignedShort.SIZE + Long.SIZE;

    /**
     * The minimum number of bytes to represent a PartialEntry.
     */
    static final int BYTES = SIZE / Byte.SIZE;

    static PartialEntry fromBytes(byte[] bytes) {
      byte[] keyLengthBytes = Arrays.copyOfRange(bytes, 0, UnsignedShort.BYTES);
      UnsignedShort keyLength = UnsignedShort.fromBytes(keyLengthBytes);

      int offsetEnd = Long.BYTES + UnsignedShort.BYTES;
      byte[] offsetBytes = Arrays.copyOfRange(bytes, UnsignedShort.BYTES, offsetEnd);
      long offset = Longs.fromByteArray(offsetBytes);

      return new PartialEntry(keyLength, offset);
    }

    byte[] getBytes() {
      byte[] keyLengthBytes = keyLength.getBytes();
      byte[] offsetBytes = Longs.toByteArray(offset);

      return Bytes.concat(keyLengthBytes, offsetBytes);
    }
  }
}
