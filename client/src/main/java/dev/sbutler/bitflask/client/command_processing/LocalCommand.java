package dev.sbutler.bitflask.client.command_processing;

public sealed interface LocalCommand extends ClientCommand {

  final class Help implements LocalCommand {

    public static boolean commandStringMatches(String command) {
      return "HELP".equalsIgnoreCase(command.trim());
    }
  }

  final class Exit implements LocalCommand {

    public static boolean commandStringMatches(String command) {
      return "EXIT".equalsIgnoreCase(command.trim());
    }
  }
}
