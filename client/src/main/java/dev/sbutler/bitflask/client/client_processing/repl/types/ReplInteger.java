package dev.sbutler.bitflask.client.client_processing.repl.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents a Repl Integer that is guaranteed to support 64-bit integers.
 *
 * <p>Initializing with a null value will cause a {@link NullPointerException}.
 */
public final class ReplInteger extends ReplElement {

  private final Number value;

  public ReplInteger(Long value) {
    checkNotNull(value, "A ReplInteger cannot have a null value");
    this.value = value;
  }

  public ReplInteger(Integer value) {
    checkNotNull(value, "A ReplInteger cannot have a null value");
    this.value = value.longValue();
  }

  public ReplInteger(Short value) {
    checkNotNull(value, "A ReplInteger cannot have a null value");
    this.value = value.longValue();
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

  @Override
  public String getAsString() {
    return value.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplInteger that = (ReplInteger) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
