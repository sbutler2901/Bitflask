package bitflask.server.processing;

import bitflask.server.storage.Storage;
import java.io.IOException;

public class CommandProcessor {

  private final Storage storage;

  public CommandProcessor(Storage storage) {
    this.storage = storage;
  }

  public String processServerCommand(ServerCommand serverCommand) throws IOException {
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
