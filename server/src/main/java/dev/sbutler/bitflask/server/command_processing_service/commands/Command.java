package dev.sbutler.bitflask.server.command_processing_service.commands;

import java.util.List;

enum Command {
  PING,
  GET,
  SET;

  public static boolean isValidCommandArgs(Command command, List<String> args) {
    if (command == null) {
      return false;
    }
    return switch (command) {
      case GET -> args != null && args.size() == 1;
      case SET -> args != null && args.size() == 2;
      case PING -> args == null || args.size() == 0;
    };
  }
}
