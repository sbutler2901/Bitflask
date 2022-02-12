package dev.sbutler.bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}