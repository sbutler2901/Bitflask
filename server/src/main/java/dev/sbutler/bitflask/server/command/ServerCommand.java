package dev.sbutler.bitflask.server.command;

import dev.sbutler.bitflask.storage.commands.ClientCommandResults;

/** A general interface representing any command executed by the server. */
public interface ServerCommand {

  /** Executes the corresponding command */
  ClientCommandResults execute();
}
