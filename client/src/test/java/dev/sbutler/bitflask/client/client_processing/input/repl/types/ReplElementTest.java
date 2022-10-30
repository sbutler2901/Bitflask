package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReplElementTest {

  @Test
  void replString() {
    // Arrange
    ReplElement replElement = new ReplString("value");
    // Assert
    assertThat(replElement.isReplString()).isTrue();
    assertDoesNotThrow(replElement::getAsReplString);
  }

  @Test
  void replInteger() {
    // Arrange
    ReplElement replElement = new ReplInteger(0);
    // Assert
    assertThat(replElement.isReplInteger()).isTrue();
    assertDoesNotThrow(replElement::getAsReplInteger);
  }

  @Test
  void replString_throwsIllegalStateException() {
    // Arrange
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    ReplInteger replInteger = new ReplInteger(0);
    // Assert
    assertThrows(IllegalStateException.class, replSingleQuotedString::getAsReplString);
    assertThrows(IllegalStateException.class, replDoubleQuotedString::getAsReplString);
    assertThrows(IllegalStateException.class, replInteger::getAsReplString);
  }

  @Test
  void replInteger_throwsIllegalStateException() {
    // Arrange
    ReplString replString = new ReplString("value");
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    // Assert
    assertThrows(IllegalStateException.class, replString::getAsReplInteger);
    assertThrows(IllegalStateException.class, replSingleQuotedString::getAsReplInteger);
    assertThrows(IllegalStateException.class, replDoubleQuotedString::getAsReplInteger);
  }
}
