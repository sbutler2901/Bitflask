package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;

/** A {@link ServerCommand} that interacts with storage. */
public final class ServerStorageCommand implements ServerCommand {

  private final ClientCommand clientCommand;

  ServerStorageCommand(ClientCommand clientCommand) {
    this.clientCommand = clientCommand;
  }

  @Override
  public ClientCommandResults execute() {
    return clientCommand.execute();
  }
}
