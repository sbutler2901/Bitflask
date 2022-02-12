package bitflask.server;

import bitflask.resp.RespArray;
import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

public class ServerCommand {

  @Getter
  private final ServerCommands command;
  @Getter
  private final List<String> args;

  public ServerCommand(@NonNull RespType<?> commandMessage) {
    if (!(commandMessage instanceof RespArray clientMessageRespArray)) {
      throw new IllegalArgumentException("Message must be a RespArray");
    }

    ServerCommands command = null;
    List<String> args = new ArrayList<>();

    for (RespType<?> clientArg : clientMessageRespArray.getValue()) {
      if (!(clientArg instanceof RespBulkString clientArgBulkString)) {
        throw new IllegalArgumentException("Message args must be RespBulkStrings");
      }

      if (command == null) {
        command = ServerCommands.valueOf(clientArgBulkString.getValue().trim().toUpperCase());
      } else {
        args.add(clientArgBulkString.getValue());
      }
    }

    if (!ServerCommands.isValidCommandArgs(command, args)) {
      throw new IllegalArgumentException("Invalid arguments for the command: " + command);
    }

    this.command = command;
    this.args = args;
  }
}
