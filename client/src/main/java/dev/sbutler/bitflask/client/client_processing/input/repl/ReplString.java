package dev.sbutler.bitflask.client.client_processing.input.repl;

public final class ReplString extends ReplElement {

  private final String value;

  public ReplString(String value) {
    this.value = value;
  }

  public String getAsString() {
    return value;
  }
}
