package dev.sbutler.bitflask.client.client_processing.repl.types;

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
  void replString_throwsIllegalStateException() {
    // Arrange
    ReplString replString = new ReplString("value");
    // Assert
    assertThrows(IllegalStateException.class, replString::getAsReplInteger);
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
  void replInteger_getAsString() {
    // Arrange
    ReplInteger replInteger = new ReplInteger(1234);
    // Assert
    assertThat(replInteger.getAsString()).isEqualTo("1234");
    assertThrows(IllegalStateException.class, replInteger::getAsReplString);
  }
}
