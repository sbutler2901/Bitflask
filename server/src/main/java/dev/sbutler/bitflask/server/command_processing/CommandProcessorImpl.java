package dev.sbutler.bitflask.server.command_processing;

import dev.sbutler.bitflask.storage.Storage;
import java.io.IOException;
import java.util.Objects;
import javax.inject.Inject;

class CommandProcessorImpl implements CommandProcessor {

  private final Storage storage;

  @Inject
  CommandProcessorImpl(Storage storage) {
    this.storage = storage;
  }

  public String processServerCommand(ServerCommand serverCommand) throws IOException {
    Objects.requireNonNull(serverCommand);
    return switch (serverCommand.command()) {
      case GET -> processGetCommand(serverCommand);
      case SET -> processSetCommand(serverCommand);
      case PING -> "pong";
    };
  }

  private String processGetCommand(ServerCommand getCommand) {
    String key = getCommand.args().get(0);
    return storage.read(key).orElse("Not Found");
  }

  private String processSetCommand(ServerCommand setCommand) throws IOException {
    String key = setCommand.args().get(0);
    String value = setCommand.args().get(1);
    storage.write(key, value);
    return "OK";
  }
}
