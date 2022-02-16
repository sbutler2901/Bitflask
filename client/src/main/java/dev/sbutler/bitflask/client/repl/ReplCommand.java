package dev.sbutler.bitflask.client.repl;

public enum ReplCommand {
  EXIT,
  TEST,
  HELP;

  public static boolean isReplCommand(String command) {
    if (command == null) {
      return false;
    }
    String compareString = command.trim().toUpperCase();
    return compareString.equals(EXIT.toString())
        || compareString.equals(TEST.toString())
        || compareString.equals(HELP.toString());
  }
}
