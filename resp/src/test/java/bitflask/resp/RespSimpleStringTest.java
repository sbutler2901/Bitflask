package bitflask.resp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RespSimpleStringTest {

  @Test
  void getValue() {
    String value = "simple-string";
    RespSimpleString simpleString = new RespSimpleString(value);
    assertEquals(value, simpleString.getValue());
  }

  @Test
  void getEncodedBytes() {
    String value = "simple";
    RespSimpleString simpleString = new RespSimpleString(value);
    byte[] expected = new byte[]{(byte) RespSimpleString.TYPE_PREFIX, 's', 'i', 'm', 'p', 'l', 'e',
        RespType.CR, RespType.LF};
    assertArrayEquals(expected, simpleString.getEncodedBytes());
  }
}