package dev.sbutler.bitflask.resp.network;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.resp.types.RespSimpleString;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ProtocolException;
import org.junit.jupiter.api.Test;

public class RespReaderTest {

  @Test
  void exception_EOFException() throws Exception {
    // Arrange
    Reader reader = mock(Reader.class);
    RespReader respReader = new RespReader(reader);
    when(reader.read()).thenReturn(-1);
    // Act
    EOFException e =
        assertThrows(EOFException.class, respReader::readNextRespElement);
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("parse next RespElement");
  }

  @Test
  void exception_ProtocolException() throws Exception {
    // Arrange
    Reader reader = mock(Reader.class);
    RespReader respReader = new RespReader(reader);
    when(reader.read()).thenReturn(Integer.valueOf('a'));
    // Act
    ProtocolException e =
        assertThrows(ProtocolException.class, respReader::readNextRespElement);
    // Act
    assertThat(e).hasMessageThat().ignoringCase().contains("code not recognized");
  }

  @Test
  void respSimpleString() throws Exception {
    // Arrange
    String expected = "simple-string";
    RespSimpleString respElement = new RespSimpleString(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespSimpleString()).isTrue();
    assertThat(res.getAsRespSimpleString().getValue()).isEqualTo(expected);
  }

  @Test
  void respBulkString() throws Exception {
    // Arrange
    String expected = "simple-string";
    RespBulkString respElement = new RespBulkString(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespBulkString());
    assertThat(res.getAsRespBulkString().getValue()).isEqualTo(expected);
  }

  @Test
  void respBulkString_newline() throws Exception {
    // Arrange
    String expected = "simple\nstring";
    RespBulkString respElement = new RespBulkString(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespBulkString());
    assertThat(res.getAsRespBulkString().getValue()).isEqualTo(expected);
  }

  @Test
  void respBulkString_null() throws Exception {
    // Arrange
    RespBulkString respElement = new RespBulkString(null);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespBulkString());
    assertThat(res.getAsRespBulkString().getValue()).isNull();
  }

  @Test
  void respBulkString_carriageReturnWithoutLineFeed() throws Exception {
    // Arrange
    String expected = "simple\rstring";
    RespBulkString respElement = new RespBulkString(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespBulkString());
    assertThat(res.getAsRespBulkString().getValue()).isEqualTo(expected);
  }

  @Test
  void respBulkString_carriageReturnTwice() throws Exception {
    // Arrange
    String expected = "simple\r\rstring";
    RespBulkString respElement = new RespBulkString(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespBulkString());
    assertThat(res.getAsRespBulkString().getValue()).isEqualTo(expected);
  }

  @Test
  void respBulkString_ProtocolException() throws Exception {
    // Arrange
    Reader reader = mock(Reader.class);
    RespReader respReader = new RespReader(reader);
    when(reader.read())
        .thenReturn(Integer.valueOf(RespBulkString.TYPE_PREFIX))
        .thenReturn(Integer.valueOf('0'))
        .thenReturn(Integer.valueOf('\r'))
        .thenReturn(Integer.valueOf('\n'))
        .thenReturn(Integer.valueOf('s'))
        .thenReturn(Integer.valueOf('\r'))
        .thenReturn(Integer.valueOf('\n'));
    // Act
    ProtocolException e =
        assertThrows(ProtocolException.class, respReader::readNextRespElement);
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("length didn't match");
  }

  @Test
  void respInteger() throws Exception {
    // Arrange
    long expected = 1000;
    RespInteger respElement = new RespInteger(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespInteger()).isTrue();
    assertThat(res.getAsRespInteger().getValue()).isEqualTo(expected);
  }

  @Test
  void respError() throws Exception {
    // Arrange
    String expected = "error";
    RespError respElement = new RespError(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespError()).isTrue();
    assertThat(res.getAsRespError().getValue()).isEqualTo(expected);
  }

  @Test
  void respArray() throws Exception {
    // Arrange
    ImmutableList<RespElement> expected =
        ImmutableList.of(new RespInteger(0));
    RespArray respElement = new RespArray(expected);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespArray());
    RespArray respArray = res.getAsRespArray();
    assertThat(respArray.size()).isEqualTo(expected.size());
    assertThat(respArray.getValue()).isEqualTo(expected);
  }

  @Test
  void respArray_null() throws Exception {
    // Arrange
    RespArray respElement = new RespArray(null);
    RespReader respReader =
        createRespReaderWithRespElementSeeded(respElement);
    // Act
    RespElement res = respReader.readNextRespElement();
    // Assert
    assertThat(res.isRespArray());
    assertThat(res.getAsRespArray().getValue()).isNull();
  }

  private static RespReader createRespReaderWithRespElementSeeded(RespElement respElement) {
    InputStream is = new ByteArrayInputStream(respElement.getEncodedBytes());
    Reader reader = new InputStreamReader(is);
    return new RespReader(reader);
  }

}
