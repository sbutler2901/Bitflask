package dev.sbutler.bitflask.server.command_processing_service.commands;

import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.util.ArrayList;
import java.util.List;

class CommandUtils {

  private CommandUtils() {
  }

  static CommandType getCommandFromMessage(List<RespType<?>> commandMessageArgs) {
    if (!(commandMessageArgs.get(0) instanceof RespBulkString commandBulkString)) {
      throw new IllegalArgumentException("Message args must be RespBulkStrings");
    }

    String normalizedCommandString = commandBulkString.getValue().trim().toUpperCase();
    try {
      return CommandType.valueOf(normalizedCommandString);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown command: " + normalizedCommandString);
    }
  }

  static List<String> getArgsFromMessage(List<RespType<?>> commandMessageArgs) {
    List<String> args = new ArrayList<>();

    for (int i = 1; i < commandMessageArgs.size(); i++) {
      if (!(commandMessageArgs.get(i) instanceof RespBulkString clientArgBulkString)) {
        throw new IllegalArgumentException("Message args must be RespBulkStrings");
      }
      args.add(clientArgBulkString.getValue());
    }

    return args;

  }
}
