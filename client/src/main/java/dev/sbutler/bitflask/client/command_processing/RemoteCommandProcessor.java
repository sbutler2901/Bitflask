package dev.sbutler.bitflask.client.command_processing;

public interface RemoteCommandProcessor {

  String runCommand(RemoteCommand command) throws ProcessingException;

}
