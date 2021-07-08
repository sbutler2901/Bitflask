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
    switch (command) {
      case GET:
        return args != null && args.size() == 1;
      case SET:
        return args != null && args.size() == 2;
      case PING:
        return args == null || args.size() == 0;
      default:
        return false;
    }
  }

  public boolean isValidCommandArgs(List<String> args) {
    return isValidCommandArgs(this, args);
  }
}
