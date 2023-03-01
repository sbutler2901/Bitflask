package dev.sbutler.bitflask.storage.segmentV1;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;

/**
 * Handles encoding and decoding storage data into and from bytes.
 *
 * <p> The encoding scheme of an entry consists of:
 * <pre>
 *   | header (1 byte) | key's length (1 byte) | key (X bytes) | value's length (1 byte) | value (X bytes) |
 * </pre>
 *
 * <p>The 1 byte header allows encoding 256 descriptions of an entry. The 1 byte key and value
 * length limits those entities' length to a maximum of 255 characters.
 */
class Encoder {

  /**
   * The types of headers supported for encoding
   */
  enum Header {
    KEY_VALUE((byte) 0),
    DELETED((byte) 1);

    private final byte byteMap;

    Header(byte byteMap) {
      this.byteMap = byteMap;
    }

    byte getByteMap() {
      return byteMap;
    }

    static Header byteToHeaderMapper(byte headerByte) {
      for (Header header : Header.values()) {
        if (header.byteMap == headerByte) {
          return header;
        }
      }
      throw new IllegalArgumentException(
          String.format("Header type not found for the provided byte [%d]", (int) headerByte));
    }
  }

  /**
   * The offsets of each entity encoded
   */
  record Offsets(
      long header,
      long keyLength,
      long key,
      long valueLength,
      long value) {

  }

  /**
   * Partial offsets of an entity
   */
  record PartialOffsets(
      long header,
      long keyLength,
      long key
  ) {

  }

  private static final String ENCODING_FORMAT = "%c%c%s%c%s";

  /**
   * Encodes a header, key, and value into bytes.
   */
  static byte[] encode(Header header, String key, String value) {
    verifyKey(key);
    verifyValue(value);
    return convertToBytes(header, key, value);
  }

  /**
   * Encodes a header and key into bytes
   */
  static byte[] encodeNoValue(Header header, String key) {
    verifyKey(key);
    return convertToBytes(header, key, "");
  }

  /**
   * Performs core encoding.
   */
  private static byte[] convertToBytes(Header header, String key, String value) {
    char keyLength = (char) key.length();
    char valueLength = (char) value.length();
    String encoded = ENCODING_FORMAT
        .formatted(header.byteMap, keyLength, key, valueLength, value);

    return encoded.getBytes(StandardCharsets.UTF_8);
  }

  private static void verifyKey(String key) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() < 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
  }

  private static void verifyValue(String value) {
    checkNotNull(value);
    checkArgument(!value.isBlank(), "Expected non-blank key, but was [%s]", value);
    checkArgument(value.length() < 256, "Expect key smaller than 256 characters, but was [%d]",
        value.length());
  }

  /**
   * Decodes the offsets of an entry.
   */
  static Offsets decode(long entryOffset, int keyLength) {
    long keyLengthOffset = entryOffset + 1;
    long keyOffset = keyLengthOffset + 1;
    long valueLengthOffset = keyOffset + keyLength;
    long valueOffset = valueLengthOffset + 1;
    return new Offsets(
        entryOffset,
        keyLengthOffset,
        keyOffset,
        valueLengthOffset,
        valueOffset
    );
  }

  /**
   * Partially decodes the offsets when only the entry's offset is available.
   */
  static PartialOffsets decodePartial(long entryOffset) {
    long keyLengthOffset = entryOffset + 1;
    long keyOffset = keyLengthOffset + 1;
    return new PartialOffsets(
        entryOffset,
        keyLengthOffset,
        keyOffset);
  }

  /**
   * Gets the offset after the end of an entity.
   *
   * <p>This can be used to get the starting offset of an entity after the one provided.
   *
   * @param entityOffsets       the current entity's offsets from which the following entity's
   *                            offset should be found
   * @param lengthOfEntityValue the character length of the current entity's value field.
   *                            <strong>NOT</strong> the offset of the current entity's value
   */
  static long getNextOffsetAfterEntity(Offsets entityOffsets, int lengthOfEntityValue) {
    return entityOffsets.value + lengthOfEntityValue;
  }

  private Encoder() {

  }
}