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
}
