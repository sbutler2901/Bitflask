package dev.sbutler.bitflask.utilities;

/**
 * The various commands recognized by the REPL
 */
public enum Commands {
  EXIT,
  GET,
  SET,
  LOG,
  TEST,
  INVALID;

  public static Commands from(String command) {
    String upperCaseCommand = command.toUpperCase().trim();
    return switch (upperCaseCommand) {
      case "EXIT" -> EXIT;
      case "GET" -> GET;
      case "SET" -> SET;
      case "LOG" -> LOG;
      case "TEST" -> TEST;
      default -> INVALID;
    };
  }
}
