package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sbutler.bitflask.storage.segment.Encoder.Header;
import dev.sbutler.bitflask.storage.segment.Encoder.Offsets;
import dev.sbutler.bitflask.storage.segment.Encoder.PartialOffsets;
import org.junit.jupiter.api.Test;

public class EncoderTest {

  @Test
  public void encode() {
    // Arrange
    Header header = Header.KEY_VALUE;
    String key = "k";
    String value = "v";
    // Act
    byte[] encoded = Encoder.encode(header, key, value);
    // Assert
    assertEquals(5, encoded.length);
    assertEquals(header, Header.byteToHeaderMapper(encoded[0]));
    assertEquals(1, encoded[1]);
    assertEquals('k', encoded[2]);
    assertEquals(1, encoded[3]);
    assertEquals('v', encoded[4]);
  }

  @Test
  public void encodeNoValue() {
    // Arrange
    Header header = Header.DELETED;
    String key = "k";
    // Act
    byte[] encoded = Encoder.encodeNoValue(header, key);
    // Assert
    assertEquals(4, encoded.length);
    assertEquals(header, Header.byteToHeaderMapper(encoded[0]));
    assertEquals(1, encoded[1]);
    assertEquals('k', encoded[2]);
    assertEquals(0, encoded[3]);
  }

  @Test
  public void decoded() {
    // Arrange
    long offset = 0;
    String key = "k";
    // Act
    Offsets offsets = Encoder.decode(offset, key.length());
    // Assert
    assertEquals(offsets.header(), offset);
    assertEquals(offsets.keyLength(), offsets.header() + 1);
    assertEquals(offsets.key(), offsets.keyLength() + 1);
    assertEquals(offsets.valueLength(), offsets.key() + 1);
    assertEquals(offsets.value(), offsets.valueLength() + 1);
  }

  @Test
  public void decodePartial() {
    // Arrange
    long offset = 0;
    // Act
    PartialOffsets partialOffsets = Encoder.decodePartial(offset);
    // Assert
    assertEquals(partialOffsets.header(), offset);
    assertEquals(partialOffsets.keyLength(), partialOffsets.header() + 1);
    assertEquals(partialOffsets.key(), partialOffsets.keyLength() + 1);
  }

  @Test
  public void getNextOffsetAfterEntity() {
    // Arrange
    long offset = 0;
    String key = "k";
    String value = "v";
    Offsets offsets = Encoder.decode(offset, key.length());
    // Act
    long nextOffsetAfterEntity = Encoder.getNextOffsetAfterEntity(offsets, value.length());
    // Assert
    assertEquals(nextOffsetAfterEntity, offsets.value() + value.length());
  }

  static class HeaderTest {

    @Test
    public void byteToHeaderMapper_keyValue() {
      // Act
      Header header = Header.byteToHeaderMapper((byte) 0);
      // Assert
      assertEquals(Header.KEY_VALUE, header);
    }

    @Test
    public void byteToHeaderMapper_deleted() {
      // Act
      Header header = Header.byteToHeaderMapper((byte) 1);
      // Assert
      assertEquals(Header.DELETED, header);
    }

    @Test
    public void byteToHeaderMapper_invalidByte() {
      // Act
      IllegalArgumentException e = assertThrows(
          IllegalArgumentException.class,
          () -> Header.byteToHeaderMapper((byte) 3));
      // Assert
      assertTrue(e.getMessage().contains("type not found"));
    }

    @Test
    public void getByteMap() {
      assertEquals(0, Header.KEY_VALUE.getByteMap());
      assertEquals(1, Header.DELETED.getByteMap());
    }
  }
}
