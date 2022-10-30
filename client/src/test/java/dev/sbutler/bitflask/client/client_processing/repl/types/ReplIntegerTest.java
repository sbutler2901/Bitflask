package dev.sbutler.bitflask.client.client_processing.repl.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReplIntegerTest {

  @Test
  void create() {
    // Arrange
    Long longValue = 0L;
    Integer intValue = 1;
    Short shortValue = 2;
    // Act
    ReplInteger longReplInteger = new ReplInteger(longValue);
    ReplInteger intReplInteger = new ReplInteger(intValue);
    ReplInteger shortReplInteger = new ReplInteger(shortValue);
    // Assert
    assertThat(longReplInteger.getAsLong()).isEqualTo(longValue);
    assertThat(intReplInteger.getAsInt()).isEqualTo(intValue);
    assertThat(shortReplInteger.getAsShort()).isEqualTo(shortValue);
    assertThat(longReplInteger.getAsNumber().longValue()).isEqualTo(longValue);
  }

  @Test
  void create_nullValue_throws() {
    Long nullLong = null;
    // Act
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new ReplInteger(nullLong));
    // Assert
    assertThat(exception).hasMessageThat().contains("ReplInteger");
  }

  @Test
  void equals_differentObject() {
    // Arrange
    ReplInteger replInteger0 = new ReplInteger(0L);
    ReplInteger replInteger1 = new ReplInteger(0L);
    // Act / Assert
    assertThat(replInteger0).isEqualTo(replInteger1);
  }

  @Test
  void equals_sameObject() {
    // Arrange
    ReplInteger replInteger = new ReplInteger(0L);
    // Act / Assert
    assertThat(replInteger.equals(replInteger)).isTrue();
  }

  @Test
  void equals_null() {
    // Arrange
    ReplInteger replInteger = new ReplInteger(0L);
    // Act / Assert
    assertThat(replInteger.equals(null)).isFalse();
  }
}
