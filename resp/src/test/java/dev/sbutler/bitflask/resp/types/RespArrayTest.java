package dev.sbutler.bitflask.resp.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class RespArrayTest {

  @Test
  void getValue() {
    List<RespType<?>> inputValues = new ArrayList<>();
    inputValues.add(new RespInteger(1));
    inputValues.add(new RespBulkString("bulk"));
    RespArray respArray = new RespArray(inputValues);
    assertEquals(inputValues, respArray.getValue());
  }

  @Test
  void getValue_null() {
    RespArray respArray = new RespArray(null);
    assertNull(respArray.getValue());
  }

  @Test
  void getEncodedBytes() {
    List<RespType<?>> inputValues = new ArrayList<>();
    inputValues.add(new RespInteger(1));
    inputValues.add(new RespInteger(2));
    inputValues.add(new RespInteger(3));
    RespArray respArray = new RespArray(inputValues);
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '3', RespType.CR, RespType.LF,
        RespInteger.TYPE_PREFIX, '1', RespType.CR, RespType.LF,
        RespInteger.TYPE_PREFIX, '2', RespType.CR, RespType.LF,
        RespInteger.TYPE_PREFIX, '3', RespType.CR, RespType.LF,
    };
    byte[] res = respArray.getEncodedBytes();
    assertArrayEquals(expected, res);
  }

  @Test
  void getEncodedBytes_nestedArray() {
    List<RespType<?>> inputValues = new ArrayList<>();
    inputValues.add(new RespInteger(1));
    inputValues.add(new RespBulkString("bulk"));
    inputValues.add(new RespArray(List.of(new RespBulkString("nested"))));
    RespArray respArray = new RespArray(inputValues);
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '3', RespType.CR, RespType.LF,
        RespInteger.TYPE_PREFIX, '1', RespType.CR, RespType.LF,
        RespBulkString.TYPE_PREFIX, '4', RespType.CR, RespType.LF,
        'b', 'u', 'l', 'k', RespType.CR, RespType.LF,
        RespArray.TYPE_PREFIX, '1', RespType.CR, RespType.LF,
        RespBulkString.TYPE_PREFIX, '6', RespType.CR, RespType.LF,
        'n', 'e', 's', 't', 'e', 'd', RespType.CR, RespType.LF,
    };
    byte[] res = respArray.getEncodedBytes();
    assertArrayEquals(expected, res);
  }

  @Test
  void getEncodedBytes_empty() {
    RespArray respArray = new RespArray(new ArrayList<>());
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '0', RespType.CR, RespType.LF
    };
    assertArrayEquals(expected, respArray.getEncodedBytes());
  }

  @Test
  void getEncodedBytes_null() {
    RespArray respArray = new RespArray(null);
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '-', '1', RespType.CR, RespType.LF
    };
    assertArrayEquals(expected, respArray.getEncodedBytes());
  }

  @Test
  void toStringTest() {
    List<RespType<?>> list = new ArrayList<>(List.of(new RespInteger(1)));
    RespArray respArray = new RespArray(list);
    String expected = "[1]";
    assertEquals(expected, respArray.toString());

    list.add(new RespInteger(2));
    expected = "[1, 2]";
    respArray = new RespArray(list);
    assertEquals(expected, respArray.toString());
  }

  @Test
  void equalsTest() {
    List<RespType<?>> list = List.of(new RespInteger(1));
    RespArray first = new RespArray(list);
    assertEquals(first, first);
    assertNotEquals(null, first);
    assertNotEquals(first, new RespBulkString("other"));
    assertEquals(first, new RespArray(list));
  }

  @Test
  void hashcodeTest() {
    List<RespType<?>> list = new ArrayList<>(List.of(new RespInteger(1)));
    RespArray respArray = new RespArray(list);
    assertEquals(Objects.hash(list), respArray.hashCode());
  }
}