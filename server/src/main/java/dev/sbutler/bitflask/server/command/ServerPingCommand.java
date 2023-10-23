package dev.sbutler.bitflask.server.command;

import dev.sbutler.bitflask.storage.commands.ClientCommandResults;

/** Process the server side ping command. */
public final class ServerPingCommand implements ServerCommand {

  @Override
  public ClientCommandResults execute() {
    return new ClientCommandResults.Success("pong");
  }
}
