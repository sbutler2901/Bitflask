package dev.sbutler.bitflask.server;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;

/** A general interface representing any command executed by the server. */
public sealed interface ServerCommand
    permits ServerCommand.StorageCommand, ServerCommand.PingCommand {

  /** Executes the corresponding command */
  ClientCommandResults execute();

  /** A {@link ServerCommand} that interacts with storage. */
  final class StorageCommand implements ServerCommand {

    private final ClientCommand clientCommand;

    StorageCommand(ClientCommand clientCommand) {
      this.clientCommand = clientCommand;
    }

    @Override
    public ClientCommandResults execute() {
      return clientCommand.execute();
    }
  }

  /** Process the server side ping command. */
  final class PingCommand implements ServerCommand {

    @Override
    public ClientCommandResults execute() {
      return new ClientCommandResults.Success("pong");
    }
  }
}
