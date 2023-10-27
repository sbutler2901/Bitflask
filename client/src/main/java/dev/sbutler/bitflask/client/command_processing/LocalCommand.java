package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;

/** Commands for the client unrelated to Bitflask. */
public sealed interface LocalCommand extends ClientCommand {

  /** The user has requested help. */
  final class Help implements LocalCommand {

    private final OutputWriter outputWriter;

    public Help(OutputWriter outputWriter) {
      this.outputWriter = outputWriter;
    }

    @Override
    public boolean execute() {
      outputWriter.writeWithNewLine("I can't help you.");
      return true;
    }

    public static boolean commandStringMatches(String command) {
      return "HELP".equalsIgnoreCase(command.trim());
    }
  }

  /** The user has requested for the Client to exit. */
  final class Exit implements LocalCommand {

    @Override
    public boolean execute() {
      return false;
    }

    public static boolean commandStringMatches(String command) {
      return "EXIT".equalsIgnoreCase(command.trim());
    }
  }

  /** The user has provided an invalid command. */
  final class Invalid implements LocalCommand {

    private final OutputWriter outputWriter;
    private final String message;

    public Invalid(OutputWriter outputWriter, String message) {
      this.outputWriter = outputWriter;
      this.message = message;
    }

    @Override
    public boolean execute() {
      outputWriter.writeWithNewLine(message);
      return true;
    }
  }
}
