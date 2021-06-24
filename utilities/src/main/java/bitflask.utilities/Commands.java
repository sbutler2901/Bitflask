package bitflask.utilities;

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
    switch (upperCaseCommand) {
      case "EXIT": return EXIT;
      case "GET": return GET;
      case "SET": return SET;
      case "LOG": return LOG;
      case "TEST": return TEST;
      default: return INVALID;
    }
  }
}
