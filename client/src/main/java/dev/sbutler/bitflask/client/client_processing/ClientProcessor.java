package dev.sbutler.bitflask.client.client_processing;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.LocalCommand;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Exit;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Help;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import dev.sbutler.bitflask.client.command_processing.RemoteCommand;
import dev.sbutler.bitflask.client.command_processing.RemoteCommandProcessor;
import javax.inject.Inject;

/**
 * Handles accepting client input, submitting it for processing, and writing a response to the
 * client.
 *
 * <p>This class discerns between local, client-specific commands and remote commands. Processing
 * accordingly.
 */
public class ClientProcessor {

  private final RemoteCommandProcessor remoteCommandProcessor;
  private final OutputWriter outputWriter;

  @Inject
  ClientProcessor(RemoteCommandProcessor remoteCommandProcessor,
      OutputWriter outputWriter) {
    this.remoteCommandProcessor = remoteCommandProcessor;
    this.outputWriter = outputWriter;
  }

  /**
   * Processing the provided client input returning whether processing should continue.
   */
  public boolean processClientInput(ImmutableList<String> clientInput) {
    if (clientInput.size() == 0) {
      return true;
    }

    ClientCommand clientCommand = mapClientInputToCommand(clientInput);
    return switch (clientCommand) {
      case LocalCommand localCommand -> processLocalCommand(localCommand);
      case RemoteCommand remoteCommand -> processRemoteCommand(remoteCommand);
    };
  }

  private boolean processLocalCommand(LocalCommand localCommand) {
    switch (localCommand) {
      case Exit ignored -> {
        return false;
      }
      case Help help -> help.execute();
    }
    return true;
  }

  private boolean processRemoteCommand(RemoteCommand remoteCommand) {
    try {
      String result = remoteCommandProcessor.runCommand(remoteCommand);
      outputWriter.writeWithNewLine(result);
    } catch (ProcessingException e) {
      outputWriter.writeWithNewLine(
          "Failure to process command [" + remoteCommand.command() + "]: " + e.getMessage());
      return false;
    }
    return true;
  }

  private ClientCommand mapClientInputToCommand(ImmutableList<String> clientInput) {
    String command = clientInput.get(0);

    if (Help.commandStringMatches(command)) {
      return new Help(outputWriter);
    }
    if (Exit.commandStringMatches(command)) {
      return new Exit();
    }

    ImmutableList<String> args = clientInput.subList(1, clientInput.size());
    return new RemoteCommand(command, args);
  }
}
