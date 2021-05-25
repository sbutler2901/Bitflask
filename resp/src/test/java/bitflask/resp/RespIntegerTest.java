package bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RespIntegerTest {

  @Test
  void intConstructor_success() {
    int decodedValue = 1234;
    String expectedEncoded = RespInteger.TYPE_PREFIX + "" + decodedValue + RespInteger.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespInteger.ENCODED_CHARSET);
    RespInteger respInteger = new RespInteger(decodedValue);

    assertEquals(decodedValue, respInteger.getDecodedValue());
    assertEquals(expectedEncoded, respInteger.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respInteger.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success() {
    int decodedValue = 1234;
    String expectedEncoded = RespInteger.TYPE_PREFIX + "" + decodedValue + RespInteger.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespInteger.ENCODED_CHARSET);
    RespInteger respInteger = new RespInteger(decodedValue);

    assertEquals(decodedValue, respInteger.getDecodedValue());
    assertEquals(expectedEncoded, respInteger.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respInteger.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_failure() {
    byte[] badEncodedBytes = "%".getBytes(RespSimpleString.ENCODED_CHARSET);
    assertThrows(IllegalArgumentException.class, () -> new RespInteger(badEncodedBytes));
  }
}