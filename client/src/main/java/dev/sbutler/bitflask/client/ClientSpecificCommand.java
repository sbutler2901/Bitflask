package dev.sbutler.bitflask.client;

public enum ClientSpecificCommand {
  EXIT,
  LOG,
  TEST,
  HELP;

  public static boolean isClientSpecificCommand(String clientSpecificCommand) {
    if (clientSpecificCommand == null) {
      return false;
    }
    String compareString = clientSpecificCommand.trim().toUpperCase();
    return compareString.equals(EXIT.toString())
        || compareString.equals(LOG.toString())
        || compareString.equals(TEST.toString())
        || compareString.equals(HELP.toString());
  }
}
