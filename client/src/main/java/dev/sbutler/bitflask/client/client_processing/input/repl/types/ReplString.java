package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents an unquoted Repl String. No parsing was performed.
 *
 * <p>Initializing with a null value will cause a {@link NullPointerException}.
 */
public final class ReplString extends ReplElement {

  private final String value;

  public ReplString(String value) {
    checkNotNull(value, "A ReplString cannot have a null value");
    this.value = value;
  }

  @Override
  public String getAsString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplString that = (ReplString) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
