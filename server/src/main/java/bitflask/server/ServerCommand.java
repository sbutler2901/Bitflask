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

  public ServerCommand(@NonNull ServerCommands command, @NonNull List<String> args) {
    if (!command.isValidCommandArgs(args)) {
      throw new IllegalArgumentException("Invalid arguments for the command: " + command);
    }
    this.command = command;
    this.args = args;
  }

  public ServerCommand(@NonNull RespType<?> commandMessage) {
    if (!(commandMessage instanceof RespArray) ) {
      throw new IllegalArgumentException("Message must be a RespArray");
    }

    RespArray clientMessageRespArray = (RespArray) commandMessage;
    ServerCommands command = null;
    List<String> args = new ArrayList<>();

    for (RespType<?> clientArg : clientMessageRespArray.getValue()) {
      if(!(clientArg instanceof RespBulkString)) {
        throw new IllegalArgumentException("Message args must be RespBulkStrings");
      }

      RespBulkString clientArgBulkString = (RespBulkString) clientArg;
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
