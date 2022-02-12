package bitflask.server.processing;

import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import bitflask.server.storage.Storage;
import java.io.IOException;

public class CommandProcessor {

  private final Storage storage;

  public CommandProcessor(Storage storage) {
    this.storage = storage;
  }

  public RespType<?> getServerResponseToClient(RespType<?> clientMessage) throws IOException {
    System.out.printf("S: received from client %s%n", clientMessage);
    try {
      ServerCommand command = ServerCommand.valueOf(clientMessage);
      return processServerCommand(command);
    } catch (IllegalArgumentException e) {
      return new RespBulkString("Invalid command: " + e.getMessage());
    }
  }

  private RespType<?> processServerCommand(ServerCommand serverCommand) throws IOException {
    return switch (serverCommand.command()) {
      case GET -> processGetCommand(serverCommand);
      case SET -> processSetCommand(serverCommand);
      case PING -> new RespBulkString("pong");
    };
  }

  private RespType<?> processGetCommand(ServerCommand getCommand) {
    String key = getCommand.args().get(0);
    String value = storage.read(key).orElse("Not Found");
    return new RespBulkString(value);
  }

  private RespType<?> processSetCommand(ServerCommand setCommand) throws IOException {
    String key = setCommand.args().get(0);
    String value = setCommand.args().get(1);
    storage.write(key, value);
    return new RespBulkString("OK");
  }
}
