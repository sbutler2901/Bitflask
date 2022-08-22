package dev.sbutler.bitflask.client.client_processing.repl;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.client.client_processing.ClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.LocalCommand;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Exit;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Help;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import dev.sbutler.bitflask.client.command_processing.RemoteCommand;
import dev.sbutler.bitflask.client.command_processing.RemoteCommandProcessor;
import javax.inject.Inject;

public class ReplClientProcessorService extends AbstractExecutionThreadService implements
    ClientProcessorService {

  private static final String SHELL_PREFIX = "> ";

  private final RemoteCommandProcessor remoteCommandProcessor;
  private final InputParser inputParser;
  private final OutputWriter outputWriter;

  private boolean continueProcessingClientInput = true;

  @Inject
  public ReplClientProcessorService(RemoteCommandProcessor remoteCommandProcessor,
      InputParser inputParser,
      OutputWriter outputWriter) {
    this.remoteCommandProcessor = remoteCommandProcessor;
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
    ImmutableList<String> clientInput = inputParser.getClientNextInput();
    if (clientInput.size() == 0) {
      return;
    }

    ClientCommand clientCommand = mapClientInputToCommand(clientInput);
    switch (clientCommand) {
      case LocalCommand localCommand -> processLocalCommand(localCommand);
      case RemoteCommand remoteCommand -> processRemoteCommand(remoteCommand);
    }
  }

  private void processLocalCommand(LocalCommand localCommand) {
    switch (localCommand) {
      case Exit _exit -> stopProcessingClientInput();
      case Help _help -> outputWriter.writeWithNewLine("I can't help you.");
    }
  }

  private void processRemoteCommand(RemoteCommand remoteCommand) {
    try {
      String result = remoteCommandProcessor.runCommand(remoteCommand);
      outputWriter.writeWithNewLine(result);
    } catch (ProcessingException e) {
      outputWriter.writeWithNewLine(
          "Failure to process command [" + remoteCommand.command() + "]: " + e.getMessage());
      stopProcessingClientInput();
    }
  }

  private ClientCommand mapClientInputToCommand(ImmutableList<String> clientInput) {
    String command = clientInput.get(0);

    if (Help.commandStringMatches(command)) {
      return new Help();
    }
    if (Exit.commandStringMatches(command)) {
      return new Exit();
    }

    ImmutableList<String> args = clientInput.subList(1, clientInput.size());
    return new RemoteCommand(command, args);
  }

  private void stopProcessingClientInput() {
    continueProcessingClientInput = false;
  }
}
