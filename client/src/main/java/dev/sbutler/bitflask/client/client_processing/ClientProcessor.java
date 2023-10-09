package dev.sbutler.bitflask.client.client_processing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.LocalCommand;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Exit;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Help;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import dev.sbutler.bitflask.client.command_processing.RemoteCommand;
import dev.sbutler.bitflask.client.command_processing.RespCommandProcessor;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespServiceProvider;
import jakarta.inject.Inject;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Handles accepting client input, submitting it for processing, and writing a response to the
 * client.
 *
 * <p>This class discerns between local, client-specific commands and remote commands. Processing
 * accordingly.
 */
public class ClientProcessor {

  private final RespCommandProcessor respCommandProcessor;
  private final OutputWriter outputWriter;
  private final RespServiceProvider respServiceProvider;

  @Inject
  ClientProcessor(
      RespCommandProcessor respCommandProcessor,
      OutputWriter outputWriter,
      RespServiceProvider respServiceProvider) {
    this.respCommandProcessor = respCommandProcessor;
    this.outputWriter = outputWriter;
    this.respServiceProvider = respServiceProvider;
  }

  /** Processing the provided client input returning whether processing should continue. */
  public boolean processClientInput(ImmutableList<ReplElement> clientInput) {
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
      RespResponse response = respCommandProcessor.runCommand(remoteCommand);
      return handleRespResponse(response);
    } catch (ProcessingException e) {
      outputWriter.writeWithNewLine(
          "Failure to process command [" + remoteCommand.command() + "]: " + e.getMessage());
      return false;
    }
  }

  private boolean handleRespResponse(RespResponse response) {
    return switch (response) {
      case RespResponse.Success ignored -> {
        outputWriter.writeWithNewLine(response.getMessage());
        yield true;
      }
      case RespResponse.Failure ignored -> {
        outputWriter.writeWithNewLine(response.getMessage());
        yield true;
      }
      case RespResponse.NotCurrentLeader notCurrentLeader -> updateRespServiceForNewLeader(
          notCurrentLeader.getHost(), notCurrentLeader.getPort());
      case RespResponse.NoKnownLeader ignored -> {
        outputWriter.writeWithNewLine(response.getMessage());
        yield false;
      }
    };
  }

  private boolean updateRespServiceForNewLeader(String host, int port) {
    RespService respService;
    try {
      respServiceProvider.get().close();
      SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
      respService = RespService.create(socketChannel);
    } catch (Exception e) {
      outputWriter.writeWithNewLine(
          String.format("Failed to reconnect to new leader. %s", e.getMessage()));
      return false;
    }
    respServiceProvider.updateRespService(respService);
    outputWriter.writeWithNewLine(
        String.format(
            "Reconnected to the current Bitflask server leader host [%s] port [%d]. Retry your command.",
            host, port));
    return true;
  }

  private ClientCommand mapClientInputToCommand(ImmutableList<ReplElement> clientInput) {
    String command = clientInput.get(0).getAsString();

    if (Help.commandStringMatches(command)) {
      return new Help(outputWriter);
    }
    if (Exit.commandStringMatches(command)) {
      return new Exit();
    }

    ImmutableList<String> args =
        convertReplElementsToArgs(clientInput.subList(1, clientInput.size()));
    return new RemoteCommand(command, args);
  }

  private static ImmutableList<String> convertReplElementsToArgs(
      ImmutableList<ReplElement> elements) {
    return elements.stream().map(ReplElement::getAsString).collect(toImmutableList());
  }
}
