package dev.sbutler.bitflask.resp.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
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
        RespElement.CR, RespElement.LF};
    assertArrayEquals(expected, simpleString.getEncodedBytes());
  }

  @Test
  void constructor_null() {
    assertThrows(IllegalArgumentException.class, () -> new RespSimpleString(null));
  }

  @Test
  void toStringTest() {
    String expected = "test";
    RespSimpleString simpleString = new RespSimpleString(expected);
    assertEquals(expected, simpleString.toString());
  }

  @Test
  void equalsTest() {
    String value = "test";
    RespSimpleString first = new RespSimpleString(value);
    assertEquals(first, first);
    assertNotEquals(null, first);
    assertNotEquals(first, new RespBulkString("other"));
    assertEquals(first, new RespSimpleString(value));
  }

  @Test
  void hashcodeTest() {
    String expected = "test";
    RespSimpleString simpleString = new RespSimpleString(expected);
    assertEquals(Objects.hash(expected), simpleString.hashCode());
  }
}
