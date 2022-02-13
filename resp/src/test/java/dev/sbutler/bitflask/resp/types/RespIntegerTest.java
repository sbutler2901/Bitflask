package dev.sbutler.bitflask.resp.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Objects;
import org.junit.jupiter.api.Test;

class RespIntegerTest {

  @Test
  void getValue() {
    RespInteger max = new RespInteger(Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, max.getValue());
    RespInteger min = new RespInteger(Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, min.getValue());
  }

  @Test
  void getEncodedBytes() {
    RespInteger singleDigit = new RespInteger(0);
    byte[] expected = new byte[]{
        RespInteger.TYPE_PREFIX, '0', RespType.CR, RespType.LF,
    };
    byte[] res = singleDigit.getEncodedBytes();
    assertArrayEquals(expected, res);
  }

  @Test
  void toStringTest() {
    int expected = 1;
    RespInteger respInteger = new RespInteger(expected);
    assertEquals(String.valueOf(expected), respInteger.toString());
  }

  @Test
  void equalsTest() {
    int value = 1;
    RespInteger first = new RespInteger(value);
    assertEquals(first, first);
    assertNotEquals(null, first);
    assertNotEquals(first, new RespBulkString("other"));
    assertEquals(first, new RespInteger(value));
  }

  @Test
  void hashcodeTest() {
    int expected = 1;
    RespInteger respInteger = new RespInteger(expected);
    assertEquals(Objects.hash(expected), respInteger.hashCode());
  }
}