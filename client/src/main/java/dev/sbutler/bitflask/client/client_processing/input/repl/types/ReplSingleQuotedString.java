package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents a Repl single quoted string with appropriate parsing performed.
 *
 * <p>Parsing includes accepting escaped single-quotes and backslashes.
 *
 * <p>Initializing with a null value will cause a {@link NullPointerException}.
 */
public final class ReplSingleQuotedString extends ReplElement {

  private final String value;

  public ReplSingleQuotedString(String value) {
    checkNotNull(value, "A ReplSingleQuotedString cannot have a null value");
    this.value = value;
  }

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
    ReplSingleQuotedString that = (ReplSingleQuotedString) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
