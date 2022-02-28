package dev.sbutler.bitflask.server.command_processing;

import java.io.IOException;

public interface CommandProcessor {

  String processServerCommand(ServerCommand serverCommand) throws IOException;

}
