package dev.sbutler.bitflask.server.command_processing_service.commands;

import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespType;
import java.util.List;
import java.util.Objects;

public class ServerCommand {

  ServerCommand() {
  }

  public static ServerCommand valueOf(RespType<?> commandMessage) {
    Objects.requireNonNull(commandMessage);
    if (!(commandMessage instanceof RespArray clientMessageRespArray)) {
      throw new IllegalArgumentException("Message must be a RespArray");
    }

    CommandType commandType = CommandUtils.getCommandFromMessage(clientMessageRespArray.getValue());
    List<String> args = CommandUtils.getArgsFromMessage(clientMessageRespArray.getValue());

    if (!CommandType.isValidCommandArgs(commandType, args)) {
      throw new IllegalArgumentException(
          "Invalid arguments for the commandType: " + commandType + ", " + args);
    }

    return getChildCommand(commandType, args);
  }

  private static ServerCommand getChildCommand(CommandType commandType, List<String> args) {
    return switch (commandType) {
      case PING -> new PingCommand();
      case GET -> new GetCommand(args.get(0));
      case SET -> new SetCommand(args.get(0), args.get(1));
    };
  }
}
