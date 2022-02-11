package bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}