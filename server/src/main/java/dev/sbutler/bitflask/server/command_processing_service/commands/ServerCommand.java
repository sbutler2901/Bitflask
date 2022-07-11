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

    Command command = CommandUtils.getCommandFromMessage(clientMessageRespArray.getValue());
    List<String> args = CommandUtils.getArgsFromMessage(clientMessageRespArray.getValue());

    if (!Command.isValidCommandArgs(command, args)) {
      throw new IllegalArgumentException(
          "Invalid arguments for the command: " + command + ", " + args);
    }

    return getChildCommand(command, args);
  }

  private static ServerCommand getChildCommand(Command command, List<String> args) {
    return switch (command) {
      case PING -> new PingCommand();
      case GET -> new GetCommand(args.get(0));
      case SET -> new SetCommand(args.get(0), args.get(1));
    };
  }
}
