package bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RespBulkStringTest {

  @Test
  void stringConstructor_success() {
    String decodedValue = "Test";
    String expectedEncoded =
        RespBulkString.TYPE_PREFIX + "" + decodedValue.length() + RespBulkString.CRLF + decodedValue
            + RespBulkString.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespBulkString.ENCODED_CHARSET);
    RespBulkString respBulkString = new RespBulkString(decodedValue);

    assertEquals(decodedValue, respBulkString.getDecodedValue());
    assertEquals(expectedEncoded, respBulkString.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respBulkString.getEncodedBytes());
  }

  @Test
  void stringConstructor_success_emptyString() {
    String decodedValue = "";
    String expectedEncoded =
        RespBulkString.TYPE_PREFIX + "" + 0 + RespBulkString.CRLF + decodedValue
            + RespBulkString.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespBulkString.ENCODED_CHARSET);
    RespBulkString respBulkString = new RespBulkString(decodedValue);

    assertEquals(decodedValue, respBulkString.getDecodedValue());
    assertEquals(expectedEncoded, respBulkString.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respBulkString.getEncodedBytes());
  }

  @Test
  void stringConstructor_success_null() {
    String decodedValue = null;
    String expectedEncoded =
        RespBulkString.TYPE_PREFIX + RespBulkString.NULL_STRING_LENGTH + RespBulkString.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespBulkString.ENCODED_CHARSET);
    RespBulkString respBulkString = new RespBulkString(decodedValue);

    assertNull(respBulkString.getDecodedValue());
    assertEquals(expectedEncoded, respBulkString.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respBulkString.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success() {
    String decodedValue = "Test";
    String expectedEncoded =
        RespBulkString.TYPE_PREFIX + "" + decodedValue.length() + RespBulkString.CRLF + decodedValue
            + RespBulkString.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespBulkString.ENCODED_CHARSET);
    RespBulkString respBulkString = new RespBulkString(expectedEncodedBytes);

    assertEquals(decodedValue, respBulkString.getDecodedValue());
    assertEquals(expectedEncoded, respBulkString.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respBulkString.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success_emptyString() {
    String decodedValue = "";
    String expectedEncoded =
        RespBulkString.TYPE_PREFIX + "" + 0 + RespBulkString.CRLF + decodedValue
            + RespBulkString.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespBulkString.ENCODED_CHARSET);
    RespBulkString respBulkString = new RespBulkString(expectedEncodedBytes);

    assertEquals(decodedValue, respBulkString.getDecodedValue());
    assertEquals(expectedEncoded, respBulkString.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respBulkString.getEncodedBytes());

  }

  @Test
  void byteArrayConstructor_success_null() {
    String expectedEncoded =
        RespBulkString.TYPE_PREFIX + RespBulkString.NULL_STRING_LENGTH + RespBulkString.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespBulkString.ENCODED_CHARSET);
    RespBulkString respBulkString = new RespBulkString(expectedEncodedBytes);

    assertNull(respBulkString.getDecodedValue());
    assertEquals(expectedEncoded, respBulkString.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respBulkString.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_failure() {
    byte[] badEncodedBytes = "%".getBytes(RespBulkString.ENCODED_CHARSET);
    assertThrows(IllegalArgumentException.class, () -> new RespBulkString(badEncodedBytes));

  }
}