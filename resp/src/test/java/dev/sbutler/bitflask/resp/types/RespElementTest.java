package dev.sbutler.bitflask.resp.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

public class RespElementTest {

  @Test
  void respSimpleString() {
    // Arrange
    RespElement respElement = new RespSimpleString("value");
    // Assert
    assertThat(respElement.isRespSimpleString()).isTrue();
    assertDoesNotThrow(respElement::getAsRespSimpleString);
  }

  @Test
  void getAsRespSimpleString_throwsIllegalStateException() {
    // Arrange
    RespBulkString respElement = new RespBulkString("value");
    // Assert
    assertThrows(IllegalStateException.class, respElement::getAsRespSimpleString);
  }

  @Test
  void respBulkString() {
    // Arrange
    RespElement respElement = new RespBulkString("value");
    // Assert
    assertThat(respElement.isRespBulkString()).isTrue();
    assertDoesNotThrow(respElement::getAsRespBulkString);
  }

  @Test
  void getAsRespBulkString_throwsIllegalStateException() {
    // Arrange
    RespSimpleString respElement = new RespSimpleString("value");
    // Assert
    assertThrows(IllegalStateException.class, respElement::getAsRespBulkString);
  }

  @Test
  void respInteger() {
    // Arrange
    RespElement respElement = new RespInteger(100L);
    // Assert
    assertThat(respElement.isRespInteger()).isTrue();
    assertDoesNotThrow(respElement::getAsRespInteger);
  }

  @Test
  void getAsRespInteger_throwsIllegalStateException() {
    // Arrange
    RespSimpleString respElement = new RespSimpleString("value");
    // Assert
    assertThrows(IllegalStateException.class, respElement::getAsRespInteger);
  }

  @Test
  void respError() {
    // Arrange
    RespElement respElement = new RespError("error");
    // Assert
    assertThat(respElement.isRespError()).isTrue();
    assertDoesNotThrow(respElement::getAsRespError);
  }

  @Test
  void getAsRespError_throwsIllegalStateException() {
    // Arrange
    RespSimpleString respElement = new RespSimpleString("value");
    // Assert
    assertThrows(IllegalStateException.class, respElement::getAsRespError);
  }

  @Test
  void respArray() {
    // Arrange
    RespElement respElement = new RespArray(List.of());
    // Assert
    assertThat(respElement.isRespArray()).isTrue();
    assertDoesNotThrow(respElement::getAsRespArray);
  }

  @Test
  void getAsRespArray_throwsIllegalStateException() {
    // Arrange
    RespSimpleString respElement = new RespSimpleString("value");
    // Assert
    assertThrows(IllegalStateException.class, respElement::getAsRespArray);
  }
}
