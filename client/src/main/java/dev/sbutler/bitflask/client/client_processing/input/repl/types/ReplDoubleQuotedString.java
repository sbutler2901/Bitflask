package dev.sbutler.bitflask.client.client_processing.input.repl.types;

/**
 * Represents a Repl single quoted string with appropriate parsing performed.
 *
 * <p>Parsing includes accepting escaped single-quotes and backslashes.
 */
public final class ReplDoubleQuotedString extends ReplElement {

  private final String value;

  public ReplDoubleQuotedString(String value) {
    this.value = value;
  }

  public String getAsString() {
    return value;
  }
}
