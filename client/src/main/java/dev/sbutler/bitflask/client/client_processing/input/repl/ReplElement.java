package dev.sbutler.bitflask.client.client_processing.input.repl;

public abstract class ReplElement {

  public boolean isReplString() {
    return this instanceof ReplString;
  }

  public boolean isReplNumber() {
    return this instanceof ReplInteger;
  }

  public ReplString getAsReplString() {
    if (isReplString()) {
      return (ReplString) this;
    }
    throw new IllegalStateException("Not a Repl String: " + this);
  }

  public ReplInteger getAsReplNumber() {
    if (isReplNumber()) {
      return (ReplInteger) this;
    }
    throw new IllegalStateException("Not a Repl Number: " + this);
  }
}
