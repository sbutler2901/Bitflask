package dev.sbutler.bitflask.resp.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RespErrorTest {

  @Test
  void getValue() {
    String value = "error";
    RespError simpleString = new RespError(value);
    assertEquals(value, simpleString.getValue());
  }

  @Test
  void getEncodedBytes() {
    String value = "error";
    RespError simpleString = new RespError(value);
    byte[] expected = new byte[]{(byte) RespError.TYPE_PREFIX, 'e', 'r', 'r', 'o', 'r',
        RespType.CR, RespType.LF};
    assertArrayEquals(expected, simpleString.getEncodedBytes());
  }
}