package dev.sbutler.bitflask.client;

/** The mode that the Client is executing in. */
public enum ExecutionMode {
  // Interactive
  REPL,
  // Single execution
  INLINE;

  public boolean isReplMode() {
    return REPL.equals(this);
  }

  public boolean isInlineMode() {
    return INLINE.equals(this);
  }
}
