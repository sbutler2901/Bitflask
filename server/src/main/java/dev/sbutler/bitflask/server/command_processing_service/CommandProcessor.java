package dev.sbutler.bitflask.server.command_processing_service;

public interface CommandProcessor {

  String processServerCommand(ServerCommand serverCommand);

}
