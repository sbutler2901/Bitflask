package dev.sbutler.bitflask.client.command_processing;

public sealed interface ClientCommand permits LocalCommand, RemoteCommand {

  /** Returns whether execution should continue. */
  boolean execute();
}
