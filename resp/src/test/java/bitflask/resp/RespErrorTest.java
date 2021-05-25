package bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RespErrorTest {

  @Test
  void stringConstructor_success() {
    String decodedValue = "Test";
    String expectedEncoded = RespError.TYPE_PREFIX + decodedValue + RespError.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespError.ENCODED_CHARSET);
    RespError respError = new RespError(decodedValue);

    assertEquals(decodedValue, respError.getDecodedValue());
    assertEquals(expectedEncoded, respError.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respError.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success() {
    String decodedValue = "Test";
    String expectedEncoded = RespError.TYPE_PREFIX + decodedValue + RespError.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespError.ENCODED_CHARSET);
    RespError respError = new RespError(expectedEncodedBytes);

    assertEquals(decodedValue, respError.getDecodedValue());
    assertEquals(expectedEncoded, respError.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respError.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_failure() {
    byte[] badEncodedBytes = "%".getBytes(RespError.ENCODED_CHARSET);
    assertThrows(IllegalArgumentException.class, () -> new RespError(badEncodedBytes));
  }

}