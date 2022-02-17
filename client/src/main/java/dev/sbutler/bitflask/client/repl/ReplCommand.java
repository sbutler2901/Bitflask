package dev.sbutler.bitflask.client.repl;

import java.util.List;

public enum ReplCommand {
  EXIT,
  HELP;

  public static boolean isReplCommand(String command) {
    if (command == null) {
      return false;
    }
    String compareString = command.trim().toUpperCase();
    return compareString.equals(EXIT.toString())
        || compareString.equals(HELP.toString());
  }

  public static boolean isValidReplCommandWithArgs(ReplCommand replCommand, List<String> args) {
    return switch (replCommand) {
      case EXIT, HELP -> args == null || args.size() == 0;
    };
  }
}
