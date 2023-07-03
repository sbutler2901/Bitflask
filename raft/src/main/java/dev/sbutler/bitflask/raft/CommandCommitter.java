package dev.sbutler.bitflask.raft;

/** Supports committing commands. */
interface CommandCommitter {

  /** A blocking call that commits the provided {@link SetCommand} returning true if successful */
  boolean commitCommand(SetCommand setCommand);

  /**
   * A blocking call that commits the provided {@link DeleteCommand} returning true if successful
   */
  boolean commitCommand(DeleteCommand deleteCommand);
}
