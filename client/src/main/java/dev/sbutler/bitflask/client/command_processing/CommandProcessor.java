package dev.sbutler.bitflask.client.command_processing;

public interface CommandProcessor {

  String runCommand(ClientCommand command) throws ProcessingException;

}
