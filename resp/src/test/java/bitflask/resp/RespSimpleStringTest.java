//package bitflask.resp;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import org.junit.jupiter.api.Test;
//
//class RespSimpleStringTest {
//
//  @Test
//  void stringConstructor_success() {
//    String decodedValue = "Test";
//    String expectedEncoded = RespSimpleString.TYPE_PREFIX + decodedValue + RespSimpleString.CRLF;
//    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespSimpleString.ENCODED_CHARSET);
//    RespSimpleString respSimpleString = new RespSimpleString(decodedValue);
//
//    assertEquals(decodedValue, respSimpleString.getDecodedValue());
//    assertEquals(expectedEncoded, respSimpleString.getEncodedString());
//    assertArrayEquals(expectedEncodedBytes, respSimpleString.getEncodedBytes());
//  }
//
//  @Test
//  void byteArrayConstructor_success() {
//    String decodedValue = "Test";
//    String expectedEncoded = RespSimpleString.TYPE_PREFIX + decodedValue + RespSimpleString.CRLF;
//    byte[] expectedEncodedBytes = expectedEncoded.getBytes(RespSimpleString.ENCODED_CHARSET);
//    RespSimpleString respSimpleString = new RespSimpleString(expectedEncodedBytes);
//
//    assertEquals(decodedValue, respSimpleString.getDecodedValue());
//    assertEquals(expectedEncoded, respSimpleString.getEncodedString());
//    assertArrayEquals(expectedEncodedBytes, respSimpleString.getEncodedBytes());
//  }
//
//  @Test
//  void byteArrayConstructor_failure() {
//    byte[] badEncodedBytes = "%".getBytes(RespSimpleString.ENCODED_CHARSET);
//    assertThrows(IllegalArgumentException.class, () -> new RespSimpleString(badEncodedBytes));
//  }
//}