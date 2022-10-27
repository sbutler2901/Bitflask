package dev.sbutler.bitflask.client.client_processing.input.repl.types;

import static com.google.common.base.Preconditions.checkNotNull;

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

  public String getAsString() {
    return value;
  }
}
