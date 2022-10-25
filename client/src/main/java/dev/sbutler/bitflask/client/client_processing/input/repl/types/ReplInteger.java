package dev.sbutler.bitflask.client.client_processing.input.repl.types;

/**
 * Represents a Repl Integer that is guaranteed to support 64-bit integers.
 */
public final class ReplInteger extends ReplElement {

  private final Number value;

  public ReplInteger(Long value) {
    this.value = value;
  }

  public ReplInteger(Integer value) {
    this.value = value;
  }

  public ReplInteger(Short value) {
    this.value = value;
  }

  public Number getAsNumber() {
    return value;
  }

  public long getAsLong() {
    return value.longValue();
  }

  public int getAsInt() {
    return value.intValue();
  }

  public int getAsShort() {
    return value.shortValue();
  }
}
