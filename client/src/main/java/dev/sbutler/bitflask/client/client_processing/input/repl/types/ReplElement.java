package dev.sbutler.bitflask.client.client_processing.input.repl.types;

public abstract class ReplElement {

  public boolean isReplString() {
    return this instanceof ReplString;
  }

  public boolean isReplSingleQuotedString() {
    return this instanceof ReplSingleQuotedString;
  }

  public boolean isReplDoubleQuotedString() {
    return this instanceof ReplDoubleQuotedString;
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

  public ReplSingleQuotedString getAsReplSingleQuotedString() {
    if (isReplSingleQuotedString()) {
      return (ReplSingleQuotedString) this;
    }
    throw new IllegalStateException("Not a Repl SingleQuotedString: " + this);
  }

  public ReplDoubleQuotedString getAsReplDoubleQuotedString() {
    if (isReplDoubleQuotedString()) {
      return (ReplDoubleQuotedString) this;
    }
    throw new IllegalStateException("Not a Repl DoubleQuotedString: " + this);
  }

  public ReplInteger getAsReplNumber() {
    if (isReplNumber()) {
      return (ReplInteger) this;
    }
    throw new IllegalStateException("Not a Repl Number: " + this);
  }
}
