package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReplDoubleQuotedStringTest {

  @Test
  void create() {
    // Arrange
    String value = "value";
    // Act
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString(value);
    // Assert
    assertThat(replDoubleQuotedString.getAsString()).isEqualTo(value);
  }

  @Test
  void create_nullValue_throws() {
    // Act
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new ReplDoubleQuotedString(null));
    // Assert
    assertThat(exception).hasMessageThat().contains("ReplDoubleQuotedString");
  }

  @Test
  void equals_differentObject() {
    // Arrange
    ReplDoubleQuotedString replDoubleQuotedString0 = new ReplDoubleQuotedString("value");
    ReplDoubleQuotedString replDoubleQuotedString1 = new ReplDoubleQuotedString("value");
    // Act / Assert
    assertThat(replDoubleQuotedString0).isEqualTo(replDoubleQuotedString1);
  }

  @Test
  void equals_sameObject() {
    // Arrange
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    // Act / Assert
    assertThat(replDoubleQuotedString.equals(replDoubleQuotedString)).isTrue();
  }

  @Test
  void equals_null() {
    // Arrange
    ReplDoubleQuotedString replDoubleQuotedString = new ReplDoubleQuotedString("value");
    // Act / Assert
    assertThat(replDoubleQuotedString.equals(null)).isFalse();
  }
}
