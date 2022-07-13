package dev.sbutler.bitflask.server.command_processing_service;

import java.util.List;

public enum CommandType {
  PING,
  GET,
  SET;

  public static boolean isValidCommandArgs(CommandType commandType, List<String> args) {
    if (commandType == null) {
      return false;
    }
    return switch (commandType) {
      case GET -> args != null && args.size() == 1;
      case SET -> args != null && args.size() == 2;
      case PING -> args == null || args.size() == 0;
    };
  }
}
