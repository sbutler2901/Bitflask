package dev.sbutler.bitflask.server.command_processing;

import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.StorageImpl;
import java.io.IOException;
import lombok.NonNull;

public class CommandProcessorImpl implements CommandProcessor {

  private final StorageImpl storage;

  @Inject
  public CommandProcessorImpl(StorageImpl storage) {
    this.storage = storage;
  }

  public String processServerCommand(@NonNull ServerCommand serverCommand) throws IOException {
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
