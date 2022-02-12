package bitflask.server;

import java.util.List;

public enum ServerCommands {
  PING,
  GET,
  SET;

  public static boolean isValidCommandArgs(ServerCommands command, List<String> args) {
    if (command == null) {
      return false;
    }
    return switch (command) {
      case GET -> args != null && args.size() == 1;
      case SET -> args != null && args.size() == 2;
      case PING -> args == null || args.size() == 0;
    };
  }

  public boolean isValidCommandArgs(List<String> args) {
    return isValidCommandArgs(this, args);
  }
}
