package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReplSingleQuotedStringTest {

  @Test
  void create() {
    // Arrange
    String value = "value";
    // Act
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString(value);
    // Assert
    assertThat(replSingleQuotedString.getAsString()).isEqualTo(value);
  }

  @Test
  void create_nullValue_throws() {
    // Act
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new ReplSingleQuotedString(null));
    // Assert
    assertThat(exception).hasMessageThat().contains("ReplSingleQuotedString");
  }


  @Test
  void equals_differentObject() {
    // Arrange
    ReplSingleQuotedString replSingleQuotedString0 = new ReplSingleQuotedString("value");
    ReplSingleQuotedString replSingleQuotedString1 = new ReplSingleQuotedString("value");
    // Act / Assert
    assertThat(replSingleQuotedString0).isEqualTo(replSingleQuotedString1);
  }

  @Test
  void equals_sameObject() {
    // Arrange
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    // Act / Assert
    assertThat(replSingleQuotedString.equals(replSingleQuotedString)).isTrue();
  }

  @Test
  void equals_null() {
    // Arrange
    ReplSingleQuotedString replSingleQuotedString = new ReplSingleQuotedString("value");
    // Act / Assert
    assertThat(replSingleQuotedString.equals(null)).isFalse();
  }
}
