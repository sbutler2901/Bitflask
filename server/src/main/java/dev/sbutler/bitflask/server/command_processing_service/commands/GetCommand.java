package dev.sbutler.bitflask.server.command_processing_service.commands;

public class GetCommand extends ServerCommand {

  private final String key;

  GetCommand(String key) {
    this.key = key;
  }

  public String getKey() {
    return this.key;
  }
}
