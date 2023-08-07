package dev.sbutler.bitflask.storage.raft.exceptions;

import dev.sbutler.bitflask.storage.raft.RaftCommand;

/**
 * Used to indicate and error when converting between {@link RaftCommand} and {@link
 * dev.sbutler.bitflask.storage.raft.Entry}.
 */
public class RaftCommandConversionException extends RaftException {
  public RaftCommandConversionException(String message) {
    super(message);
  }
}
