package dev.sbutler.bitflask.resp.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RespBulkStringTest {

  @Test
  void getValue() {
    String value = "simple-string";
    RespBulkString bulkString = new RespBulkString(value);
    assertEquals(value, bulkString.getValue());
  }

  @Test
  void getValue_null() {
    RespBulkString bulkString = new RespBulkString(null);
    assertNull(bulkString.getValue());
  }

  @Test
  void getEncodedBytes() {
    String value = "simple";
    RespBulkString bulkString = new RespBulkString(value);
    byte[] expected = new byte[]{
        RespBulkString.TYPE_PREFIX, '6', RespType.CR, RespType.LF,
        's', 'i', 'm', 'p', 'l', 'e', RespType.CR, RespType.LF
    };
    assertArrayEquals(expected, bulkString.getEncodedBytes());
  }

  @Test
  void getEncodedBytes_null() {
    RespBulkString bulkString = new RespBulkString(null);
    byte[] expected = new byte[]{
        RespBulkString.TYPE_PREFIX, '-', '1', RespType.CR, RespType.LF
    };
    assertArrayEquals(expected, bulkString.getEncodedBytes());
  }

  @Test
  void toStringTest() {
    String expected = "test";
    RespBulkString respBulkString = new RespBulkString(expected);
    assertEquals(expected, respBulkString.toString());
  }

  @Test
  void equalsTest() {
    String value = "test";
    RespBulkString first = new RespBulkString(value);
    assertEquals(first, first);
    assertNotEquals(null, first);
    assertNotEquals(first, new RespSimpleString("other"));
    assertEquals(first, new RespBulkString(value));
  }
}