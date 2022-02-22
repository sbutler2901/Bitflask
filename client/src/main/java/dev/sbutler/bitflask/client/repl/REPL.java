package dev.sbutler.bitflask.client.repl;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.CommandProcessor;
import dev.sbutler.bitflask.client.repl.input.InputParser;
import dev.sbutler.bitflask.client.repl.output.OutputWriter;
import java.io.IOException;

public class REPL {

  private static final String SHELL_PREFIX = "> ";

  private final CommandProcessor commandProcessor;
  private final InputParser inputParser;
  private final OutputWriter outputWriter;

  private boolean continueProcessingClientInput = true;

  public REPL(CommandProcessor commandProcessor, InputParser inputParser,
      OutputWriter outputWriter) {
    this.commandProcessor = commandProcessor;
    this.inputParser = inputParser;
    this.outputWriter = outputWriter;
  }

  /**
   * Runs the REPL loop
   */
  public void start() {
    outputWriter.writeWithNewLine("Hello from client");
    while (continueProcessingClientInput) {
      outputWriter.write(SHELL_PREFIX);
      processClientInput();
    }
  }

  private void processClientInput() {
    ClientCommand clientCommand = inputParser.getNextCommand();
    if (clientCommand == null) {
      return;
    }

    if (ReplCommand.isReplCommand(clientCommand.command())) {
      processReplCommand(clientCommand);
    } else {
      processServerCommand(clientCommand);
    }
  }

  private void processReplCommand(ClientCommand clientCommand) {
    ReplCommand replCommand = ReplCommand
        .valueOf(clientCommand.command().trim().toUpperCase());
    if (!ReplCommand.isValidReplCommandWithArgs(replCommand, clientCommand.args())) {
      outputWriter.writeWithNewLine(
          "Invalid REPL command: " + replCommand + ", " + clientCommand.args());
      return;
    }

    switch (replCommand) {
      case EXIT -> haltClientProcessing();
      case HELP -> outputWriter.writeWithNewLine("I can't help you.");
    }
  }

  private void processServerCommand(ClientCommand clientCommand) {
    try {
      String result = commandProcessor.runCommand(clientCommand);
      outputWriter.writeWithNewLine(result);
    } catch (IOException e) {
      outputWriter.writeWithNewLine(
          "Failure to process command [" + clientCommand.command() + "]: " + e.getMessage());
      haltClientProcessing();
    }
  }

  private void haltClientProcessing() {
    outputWriter.writeWithNewLine("Exiting...");
    continueProcessingClientInput = false;
  }
}
