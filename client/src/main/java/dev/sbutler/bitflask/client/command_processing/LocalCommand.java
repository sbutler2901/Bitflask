package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;

public sealed interface LocalCommand extends ClientCommand {

  void execute();

  final class Help implements LocalCommand {

    private final OutputWriter outputWriter;

    public Help(OutputWriter outputWriter) {
      this.outputWriter = outputWriter;
    }

    @Override
    public void execute() {
      outputWriter.writeWithNewLine("I can't help you.");
    }

    public static boolean commandStringMatches(String command) {
      return "HELP".equalsIgnoreCase(command.trim());
    }
  }

  final class Exit implements LocalCommand {

    @Override
    public void execute() {

    }

    public static boolean commandStringMatches(String command) {
      return "EXIT".equalsIgnoreCase(command.trim());
    }
  }
}
