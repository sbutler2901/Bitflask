package dev.sbutler.bitflask.storage.segment;


import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.entry.Entry;
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

    byte[] keyLengthBytes = Arrays.copyOfRange(bytes, 0, UnsignedShort.BYTES);

    int offsetEnd = Long.BYTES + UnsignedShort.BYTES;
    byte[] offsetBytes = Arrays.copyOfRange(bytes, UnsignedShort.BYTES, offsetEnd);

    UnsignedShort keyLength = UnsignedShort.fromBytes(keyLengthBytes);
    byte[] keyBytes = Arrays.copyOfRange(bytes, offsetEnd, offsetEnd + keyLength.value());

    return new SegmentIndexEntry(new String(keyBytes), Longs.fromByteArray(offsetBytes));
  }

  byte[] getBytes() {
    byte[] keyLengthBytes = UnsignedShort.valueOf(key.length()).getBytes();
    byte[] offsetBytes = Longs.toByteArray(offset);
    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

    return Bytes.concat(keyLengthBytes, offsetBytes, keyBytes);
  }
}
