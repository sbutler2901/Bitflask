package bitflask.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RespArrayTest {

  @Test
  void listConstructor_success() {
    List<RespType> decodedValue = new ArrayList<>();
    RespSimpleString respSimpleString = new RespSimpleString("foo");
    RespInteger respInteger = new RespInteger(10);
    RespError respError = new RespError("error");
    RespBulkString respBulkString = new RespBulkString("bar");

    decodedValue.add(respSimpleString);
    decodedValue.add(respInteger);
    decodedValue.add(respError);
    decodedValue.add(respBulkString);

    StringBuilder expectedEncodedBuilder = new StringBuilder();
    expectedEncodedBuilder.append(RespArray.TYPE_PREFIX).append(decodedValue.size())
        .append(RespArray.CRLF);
    decodedValue.forEach(entry -> expectedEncodedBuilder.append(entry.getEncodedString()));

    String expectedEncoded = expectedEncodedBuilder.toString();
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArray = new RespArray(decodedValue);

    assertEquals(decodedValue, respArray.getDecodedValue());
    assertEquals(expectedEncoded, respArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respArray.getEncodedBytes());

    // nested array
    decodedValue.add(respArray);
    StringBuilder expectedEncodedNestedArrayBuilder = new StringBuilder();
    expectedEncodedNestedArrayBuilder.append(RespArray.TYPE_PREFIX).append(decodedValue.size())
        .append(RespArray.CRLF);
    decodedValue
        .forEach(entry -> expectedEncodedNestedArrayBuilder.append(entry.getEncodedString()));

    String expectedEncodedNestedArray = expectedEncodedNestedArrayBuilder.toString();
    byte[] expectedEncodedBytesNestedArray = expectedEncodedNestedArray
        .getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArrayNestedArray = new RespArray(decodedValue);

    assertEquals(decodedValue, respArrayNestedArray.getDecodedValue());
    assertEquals(expectedEncodedNestedArray, respArrayNestedArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytesNestedArray, respArrayNestedArray.getEncodedBytes());
  }

  @Test
  void listConstructor_success_emptyList() {
    List<RespType> decodedValue = new ArrayList<>();
    String expectedEncoded = RespArray.TYPE_PREFIX + "0" + RespArray.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArray = new RespArray(decodedValue);

    assertEquals(decodedValue, respArray.getDecodedValue());
    assertEquals(expectedEncoded, respArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respArray.getEncodedBytes());
  }

  @Test
  void listConstructor_success_null() {
    List<RespType> decodedValue = null;
    String expectedEncoded = RespArray.TYPE_PREFIX + RespArray.NULL_STRING_LENGTH + RespArray.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArray = new RespArray(decodedValue);

    assertEquals(decodedValue, respArray.getDecodedValue());
    assertEquals(expectedEncoded, respArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respArray.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success() {
    List<RespType> decodedValue = new ArrayList<>();
    RespSimpleString respSimpleString = new RespSimpleString("foo");
    RespInteger respInteger = new RespInteger(10);
    RespError respError = new RespError("error");
    RespBulkString respBulkString = new RespBulkString("bar");

    decodedValue.add(respSimpleString);
    decodedValue.add(respInteger);
    decodedValue.add(respError);
    decodedValue.add(respBulkString);

    StringBuilder expectedEncodedBuilder = new StringBuilder();
    expectedEncodedBuilder.append(RespArray.TYPE_PREFIX).append(decodedValue.size())
        .append(RespArray.CRLF);
    decodedValue.forEach(entry -> expectedEncodedBuilder.append(entry.getEncodedString()));

    String expectedEncoded = expectedEncodedBuilder.toString();
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArray = new RespArray(expectedEncodedBytes);

    assertEquals(decodedValue, respArray.getDecodedValue());
    assertEquals(expectedEncoded, respArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respArray.getEncodedBytes());

    // nested array
    decodedValue.add(respArray);
    StringBuilder expectedEncodedNestedArrayBuilder = new StringBuilder();
    expectedEncodedNestedArrayBuilder.append(RespArray.TYPE_PREFIX).append(decodedValue.size())
        .append(RespArray.CRLF);
    decodedValue
        .forEach(entry -> expectedEncodedNestedArrayBuilder.append(entry.getEncodedString()));

    String expectedEncodedNestedArray = expectedEncodedNestedArrayBuilder.toString();
    byte[] expectedEncodedBytesNestedArray = expectedEncodedNestedArray
        .getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArrayNestedArray = new RespArray(expectedEncodedBytesNestedArray);

    assertEquals(decodedValue, respArrayNestedArray.getDecodedValue());
    assertEquals(expectedEncodedNestedArray, respArrayNestedArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytesNestedArray, respArrayNestedArray.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success_emptyList() {
    List<RespType> decodedValue = new ArrayList<>();
    String expectedEncoded = RespArray.TYPE_PREFIX + "0" + RespArray.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArray = new RespArray(expectedEncodedBytes);

    assertEquals(decodedValue, respArray.getDecodedValue());
    assertEquals(expectedEncoded, respArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respArray.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_success_null() {
    List<RespType> decodedValue = null;
    String expectedEncoded = RespArray.TYPE_PREFIX + RespArray.NULL_STRING_LENGTH + RespArray.CRLF;
    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespArray.ENCODED_CHARSET);
    RespArray respArray = new RespArray(expectedEncodedBytes);

    assertEquals(decodedValue, respArray.getDecodedValue());
    assertEquals(expectedEncoded, respArray.getEncodedString());
    assertArrayEquals(expectedEncodedBytes, respArray.getEncodedBytes());
  }

  @Test
  void byteArrayConstructor_failure() {
    byte[] badEncodedBytes = "%".getBytes(RespArray.ENCODED_CHARSET);
    assertThrows(IllegalArgumentException.class, () -> new RespArray(badEncodedBytes));
  }
}