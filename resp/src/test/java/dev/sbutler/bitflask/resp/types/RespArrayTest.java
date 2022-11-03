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
    List<RespElement> inputValues = new ArrayList<>();
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
    List<RespElement> inputValues = new ArrayList<>();
    inputValues.add(new RespInteger(1));
    inputValues.add(new RespInteger(2));
    inputValues.add(new RespInteger(3));
    RespArray respArray = new RespArray(inputValues);
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '3', RespElement.CR, RespElement.LF,
        RespInteger.TYPE_PREFIX, '1', RespElement.CR, RespElement.LF,
        RespInteger.TYPE_PREFIX, '2', RespElement.CR, RespElement.LF,
        RespInteger.TYPE_PREFIX, '3', RespElement.CR, RespElement.LF,
    };
    byte[] res = respArray.getEncodedBytes();
    assertArrayEquals(expected, res);
  }

  @Test
  void getEncodedBytes_nestedArray() {
    List<RespElement> inputValues = new ArrayList<>();
    inputValues.add(new RespInteger(1));
    inputValues.add(new RespBulkString("bulk"));
    inputValues.add(new RespArray(List.of(new RespBulkString("nested"))));
    RespArray respArray = new RespArray(inputValues);
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '3', RespElement.CR, RespElement.LF,
        RespInteger.TYPE_PREFIX, '1', RespElement.CR, RespElement.LF,
        RespBulkString.TYPE_PREFIX, '4', RespElement.CR, RespElement.LF,
        'b', 'u', 'l', 'k', RespElement.CR, RespElement.LF,
        RespArray.TYPE_PREFIX, '1', RespElement.CR, RespElement.LF,
        RespBulkString.TYPE_PREFIX, '6', RespElement.CR, RespElement.LF,
        'n', 'e', 's', 't', 'e', 'd', RespElement.CR, RespElement.LF,
    };
    byte[] res = respArray.getEncodedBytes();
    assertArrayEquals(expected, res);
  }

  @Test
  void getEncodedBytes_empty() {
    RespArray respArray = new RespArray(new ArrayList<>());
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '0', RespElement.CR, RespElement.LF
    };
    assertArrayEquals(expected, respArray.getEncodedBytes());
  }

  @Test
  void getEncodedBytes_null() {
    RespArray respArray = new RespArray(null);
    byte[] expected = new byte[]{
        RespArray.TYPE_PREFIX, '-', '1', RespElement.CR, RespElement.LF
    };
    assertArrayEquals(expected, respArray.getEncodedBytes());
  }

  @Test
  void toStringTest() {
    List<RespElement> list = new ArrayList<>(List.of(new RespInteger(1)));
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
    List<RespElement> list = List.of(new RespInteger(1));
    RespArray first = new RespArray(list);
    assertEquals(first, first);
    assertNotEquals(null, first);
    assertNotEquals(first, new RespBulkString("other"));
    assertEquals(first, new RespArray(list));
  }

  @Test
  void hashcodeTest() {
    List<RespElement> list = new ArrayList<>(List.of(new RespInteger(1)));
    RespArray respArray = new RespArray(list);
    assertEquals(Objects.hash(list), respArray.hashCode());
  }
}
