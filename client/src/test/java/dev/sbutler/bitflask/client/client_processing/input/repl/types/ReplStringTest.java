package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReplStringTest {

  @Test
  void create() {
    // Arrange
    String value = "value";
    // Act
    ReplString replString = new ReplString(value);
    // Assert
    assertThat(replString.getAsString()).isEqualTo(value);
  }

  @Test
  void create_nullValue_throws() {
    // Act
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new ReplString(null));
    // Assert
    assertThat(exception).hasMessageThat().contains("ReplString");
  }
}
