package dev.sbutler.bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RespUtilsTest {

  @Test
  void exception_EOFException() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(-1);
    assertThrows(EOFException.class, () -> RespUtils.readNextRespType(bufferedReader));
  }

  @Test
  void exception_ProtocolException() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf('a'));
    assertThrows(ProtocolException.class, () -> RespUtils.readNextRespType(bufferedReader));
  }

  @Test
  void respSimpleString() throws IOException {
    String expected = "simple-string";
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespSimpleString.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine()).thenReturn(expected);
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespSimpleString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString() throws IOException {
    String expected = "simple-string";
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine())
        .thenReturn(String.valueOf(expected.length()))
        .thenReturn(expected);
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespBulkString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString_null() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine())
        .thenReturn(String.valueOf(-1));
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespBulkString);
    assertNull(res.getValue());
  }

  @Test
  void respBulkString_ProtocolException() throws IOException {
    String expected = "simple-string";
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine())
        .thenReturn(String.valueOf(0))
        .thenReturn(expected);
    assertThrows(ProtocolException.class, () -> RespUtils.readNextRespType(bufferedReader));
  }

  @Test
  void respInteger() throws IOException {
    int expected = 1000;
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespInteger.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine())
        .thenReturn(String.valueOf(expected));
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespInteger);
    assertEquals(expected, ((RespInteger) res).getValue());
  }

  @Test
  void respError() throws IOException {
    String expected = "error";
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespError.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine()).thenReturn(expected);
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespError);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respArray() throws IOException {
    List<RespType<?>> expected = new ArrayList<>(List.of(new RespInteger(1)));
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read())
        .thenReturn(Integer.valueOf(RespArray.TYPE_PREFIX))
        .thenReturn(Integer.valueOf(RespInteger.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine())
        .thenReturn(String.valueOf(1))
        .thenReturn(String.valueOf(1));
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespArray);
    RespArray resCasted = (RespArray) res;
    assertEquals(expected.size(), resCasted.getValue().size());
    assertEquals(expected, resCasted.getValue());
  }

  @Test
  void respArray_null() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.read()).thenReturn(Integer.valueOf(RespArray.TYPE_PREFIX));
    Mockito.when(bufferedReader.readLine()).thenReturn(String.valueOf(RespArray.NULL_ARRAY_LENGTH));
    RespType<?> res = RespUtils.readNextRespType(bufferedReader);
    assertTrue(res instanceof RespArray);
    assertNull(res.getValue());
  }
}
