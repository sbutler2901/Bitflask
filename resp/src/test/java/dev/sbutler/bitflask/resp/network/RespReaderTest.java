package dev.sbutler.bitflask.resp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.resp.types.RespSimpleString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import org.junit.jupiter.api.Test;

public class RespReaderTest {

  @Test
  void exception_EOFException() throws Exception {
    // Arrange
    BufferedReader bufferedReader = mock(BufferedReader.class);
    RespReader respReader = new RespReader(bufferedReader);
    when(bufferedReader.read()).thenReturn(-1);
    // Act
    EOFException e =
        assertThrows(EOFException.class, respReader::readNextRespType);
    // Assert
    assertTrue(e.getMessage().toLowerCase().contains("parse resptype"));
  }

  @Test
  void exception_ProtocolException() throws Exception {
    // Arrange
    BufferedReader bufferedReader = mock(BufferedReader.class);
    RespReader respReader = new RespReader(bufferedReader);
    when(bufferedReader.read()).thenReturn(Integer.valueOf('a'));
    // Act
    ProtocolException e =
        assertThrows(ProtocolException.class, respReader::readNextRespType);
    // Act
    assertTrue(e.getMessage().toLowerCase().contains("code not recognized"));
  }

  @Test
  void respSimpleString() throws Exception {
    // Arrange
    String expected = "simple-string";
    RespSimpleString respType = new RespSimpleString(expected);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespSimpleString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString() throws Exception {
    // Arrange
    String expected = "simple-string";
    RespBulkString respType = new RespBulkString(expected);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespBulkString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString_newline() throws Exception {
    // Arrange
    String expected = "simple\nstring";
    RespBulkString respType = new RespBulkString(expected);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespBulkString);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respBulkString_null() throws Exception {
    // Arrange
    RespBulkString respType = new RespBulkString(null);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespBulkString);
    assertNull(res.getValue());
  }

  @Test
  void respBulkString_ProtocolException() throws Exception {
    // Arrange
    String expected = "simple-string";
    BufferedReader bufferedReader = mock(BufferedReader.class);
    RespReader respReader = new RespReader(bufferedReader);
    when(bufferedReader.read()).thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX));
    when(bufferedReader.readLine())
        .thenReturn(String.valueOf(0))
        .thenReturn(expected);
    // Act
    ProtocolException e =
        assertThrows(ProtocolException.class, respReader::readNextRespType);
    // Assert
    assertTrue(e.getMessage().toLowerCase().contains("length didn't match"));
  }

  @Test
  void respInteger() throws Exception {
    // Arrange
    int expected = 1000;
    RespInteger respType = new RespInteger(expected);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespInteger);
    assertEquals(expected, ((RespInteger) res).getValue());
  }

  @Test
  void respError() throws Exception {
    // Arrange
    String expected = "error";
    RespError respType = new RespError(expected);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespError);
    assertEquals(expected, res.getValue());
  }

  @Test
  void respArray() throws Exception {
    // Arrange
    ImmutableList<RespType<?>> expected =
        ImmutableList.of(new RespInteger(0));
    RespArray respType = new RespArray(expected);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespArray);
    RespArray resCasted = (RespArray) res;
    assertEquals(expected.size(), resCasted.getValue().size());
    assertEquals(expected, resCasted.getValue());
  }

  @Test
  void respArray_null() throws Exception {
    // Arrange
    RespArray respType = new RespArray(null);
    RespReader respReader =
        createRespReaderWithRespTypeSeeded(respType);
    // Act
    RespType<?> res = respReader.readNextRespType();
    // Assert
    assertTrue(res instanceof RespArray);
    assertNull(res.getValue());
  }

  private static RespReader createRespReaderWithRespTypeSeeded(RespType<?> respType) {
    InputStream is = new ByteArrayInputStream(respType.getEncodedBytes());
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
    return new RespReader(bufferedReader);
  }

}
