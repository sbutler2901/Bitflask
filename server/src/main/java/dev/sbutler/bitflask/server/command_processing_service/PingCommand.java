package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.storage.commands.ClientCommandResults;

/** Process the server side ping command. */
class PingCommand implements ServerCommand {

  @Override
  public ClientCommandResults execute() {
    return new ClientCommandResults.Success("pong");
  }
}
