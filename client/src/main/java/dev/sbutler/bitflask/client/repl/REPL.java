package dev.sbutler.bitflask.client.repl;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.CommandProcessor;
import java.io.IOException;
import java.util.Scanner;

public class REPL {

  private static final String SHELL_PREFIX = "> ";

  private final CommandProcessor commandProcessor;
  private final InputParser inputParser;
  private final OutputWriter outputWriter;

  private boolean continueProcessingClientInput = true;

  /**
   * Creating a new REPL instance for accepting user command to interact with the storage engine
   *
   * @param commandProcessor for processing client provided commands on the server
   */
  public REPL(CommandProcessor commandProcessor) {
    this.commandProcessor = commandProcessor;
    this.inputParser = new InputParser(new Scanner(System.in));
    this.outputWriter = new OutputWriter(System.out);
  }

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
