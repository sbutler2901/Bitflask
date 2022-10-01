package dev.sbutler.bitflask.resp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.resp.types.RespSimpleString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RespReaderTest {

  @InjectMocks
  RespReader respReader;

  @Mock
  BufferedReader bufferedReader;

  @Test
  void exception_EOFException() throws IOException {
    when(bufferedReader.read()).thenReturn(-1);
    assertThrows(EOFException.class, () -> respReader.readNextRespType());
  }

  @Test
  void exception_ProtocolException() throws IOException {
    when(bufferedReader.read()).thenReturn(Integer.valueOf('a'));
    assertThrows(ProtocolException.class, () -> respReader.readNextRespType());
  }

  @Test
  void respSimpleString() throws IOException {
    String expected = "simple-string";
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespSimpleString.TYPE_PREFIX));
    when(bufferedReader.readLine()).thenReturn(expected);
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespSimpleString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString() throws IOException {
    String expected = "simple-string";
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    when(bufferedReader.readLine())
        .thenReturn(String.valueOf(expected.length()))
        .thenReturn(expected);
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespBulkString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString_null() throws IOException {
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    when(bufferedReader.readLine())
        .thenReturn(String.valueOf(-1));
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespBulkString);
    assertNull(res.getValue());
  }

  @Test
  void respBulkString_ProtocolException() throws IOException {
    String expected = "simple-string";
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    when(bufferedReader.readLine())
        .thenReturn(String.valueOf(0))
        .thenReturn(expected);
    assertThrows(ProtocolException.class, () -> respReader.readNextRespType());
  }

  @Test
  void respInteger() throws IOException {
    int expected = 1000;
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespInteger.TYPE_PREFIX));
    when(bufferedReader.readLine())
        .thenReturn(String.valueOf(expected));
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespInteger);
    assertEquals(expected, ((RespInteger) res).getValue());
  }

  @Test
  void respError() throws IOException {
    String expected = "error";
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespError.TYPE_PREFIX));
    when(bufferedReader.readLine()).thenReturn(expected);
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespError);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respArray() throws IOException {
    List<RespType<?>> expected = new ArrayList<>(List.of(new RespInteger(1)));
    when(bufferedReader.read())
        .thenReturn(Integer.valueOf(RespArray.TYPE_PREFIX))
        .thenReturn(Integer.valueOf(RespInteger.TYPE_PREFIX));
    when(bufferedReader.readLine())
        .thenReturn(String.valueOf(1))
        .thenReturn(String.valueOf(1));
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespArray);
    RespArray resCasted = (RespArray) res;
    assertEquals(expected.size(), resCasted.getValue().size());
    assertEquals(expected, resCasted.getValue());
  }

  @Test
  void respArray_null() throws IOException {
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespArray.TYPE_PREFIX));
    when(bufferedReader.readLine()).thenReturn(String.valueOf(RespArray.NULL_ARRAY_LENGTH));
    RespType<?> res = respReader.readNextRespType();
    assertTrue(res instanceof RespArray);
    assertNull(res.getValue());
  }
}
