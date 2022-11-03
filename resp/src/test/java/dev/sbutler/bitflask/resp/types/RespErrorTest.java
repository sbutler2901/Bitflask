package dev.sbutler.bitflask.resp.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        RespElement.CR, RespElement.LF};
    assertArrayEquals(expected, simpleString.getEncodedBytes());
  }

  @Test
  void constructor_null() {
    assertThrows(NullPointerException.class, () -> new RespError(null));
  }

  @Test
  void toStringTest() {
    String expected = "error";
    RespError respError = new RespError(expected);
    assertEquals(expected, respError.toString());
  }
}
