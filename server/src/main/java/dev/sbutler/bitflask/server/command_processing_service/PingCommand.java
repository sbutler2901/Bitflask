package dev.sbutler.bitflask.server.command_processing_service;

/** Process the server side ping command. */
class PingCommand implements ServerCommand {

  @Override
  public String execute() {
    return "pong";
  }
}
