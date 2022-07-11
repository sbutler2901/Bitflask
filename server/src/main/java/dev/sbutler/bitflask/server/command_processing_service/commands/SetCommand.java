package dev.sbutler.bitflask.server.command_processing_service.commands;

public class SetCommand extends ServerCommand {

  private final String key;
  private final String value;

  SetCommand(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return this.key;
  }

  public String getValue() {
    return this.value;
  }

}
