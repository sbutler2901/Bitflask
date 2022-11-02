package dev.sbutler.bitflask.client.client_processing.repl.types;

/**
 * The parent class of all element types that can be interpreted from client input.
 *
 * <p>Provides various utility methods for determining which subclass a ReplElement is and
 * conversion methods.
 */
public abstract class ReplElement {

  public boolean isReplString() {
    return this instanceof ReplString;
  }

  public boolean isReplInteger() {
    return this instanceof ReplInteger;
  }

  public ReplString getAsReplString() {
    if (isReplString()) {
      return (ReplString) this;
    }
    throw new IllegalStateException("Not a ReplString: " + this);
  }

  public ReplInteger getAsReplInteger() {
    if (isReplInteger()) {
      return (ReplInteger) this;
    }
    throw new IllegalStateException("Not a ReplInteger: " + this);
  }

  public abstract String getAsString();
}
