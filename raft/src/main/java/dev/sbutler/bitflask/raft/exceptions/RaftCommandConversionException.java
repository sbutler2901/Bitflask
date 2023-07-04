package dev.sbutler.bitflask.raft.exceptions;

/**
 * Used to indicate and error when converting between {@link dev.sbutler.bitflask.raft.RaftCommand}
 * and {@link dev.sbutler.bitflask.raft.Entry}.
 */
public class RaftCommandConversionException extends RaftException {
  public RaftCommandConversionException(String message) {
    super(message);
  }
}
