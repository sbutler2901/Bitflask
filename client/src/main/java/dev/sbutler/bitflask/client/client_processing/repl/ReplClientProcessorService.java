package dev.sbutler.bitflask.client.client_processing.repl;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.client.client_processing.ClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.CommandProcessor;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import javax.inject.Inject;

public class ReplClientProcessorService extends AbstractExecutionThreadService implements
    ClientProcessorService {

  private static final String SHELL_PREFIX = "> ";

  private final CommandProcessor commandProcessor;
  private final InputParser inputParser;
  private final OutputWriter outputWriter;

  private boolean continueProcessingClientInput = true;

  @Inject
  public ReplClientProcessorService(CommandProcessor commandProcessor, InputParser inputParser,
      OutputWriter outputWriter) {
    this.commandProcessor = commandProcessor;
    this.inputParser = inputParser;
    this.outputWriter = outputWriter;
  }

  @Override
  protected void run() {
    while (continueProcessingClientInput) {
      outputWriter.write(SHELL_PREFIX);
      processClientInput();
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    stopProcessingClientInput();
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
      case EXIT -> stopProcessingClientInput();
      case HELP -> outputWriter.writeWithNewLine("I can't help you.");
    }
  }

  private void processServerCommand(ClientCommand clientCommand) {
    try {
      String result = commandProcessor.runCommand(clientCommand);
      outputWriter.writeWithNewLine(result);
    } catch (ProcessingException e) {
      outputWriter.writeWithNewLine(
          "Failure to process command [" + clientCommand.command() + "]: " + e.getMessage());
      stopProcessingClientInput();
    }
  }

  private void stopProcessingClientInput() {
    continueProcessingClientInput = false;
  }
}
