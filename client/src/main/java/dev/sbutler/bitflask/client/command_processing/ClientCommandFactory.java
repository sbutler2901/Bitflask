package dev.sbutler.bitflask.client.command_processing;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespRequestCode;
import dev.sbutler.bitflask.resp.network.RespServiceProvider;
import jakarta.inject.Inject;
import java.util.Optional;

/** Handles converting client input into a {@link ClientCommand}. */
public final class ClientCommandFactory {

  private final OutputWriter outputWriter;
  private final RespCommandProcessor respCommandProcessor;
  private final RespServiceProvider respServiceProvider;

  @Inject
  ClientCommandFactory(
      OutputWriter outputWriter,
      RespCommandProcessor respCommandProcessor,
      RespServiceProvider respServiceProvider) {
    this.outputWriter = outputWriter;
    this.respCommandProcessor = respCommandProcessor;
    this.respServiceProvider = respServiceProvider;
  }

  public ClientCommand createCommand(ImmutableList<ReplElement> clientInput) {
    return createRemoteCommand(clientInput).orElseGet(() -> createLocalCommand(clientInput));
  }

  private Optional<ClientCommand> createRemoteCommand(ImmutableList<ReplElement> clientInput) {
    return createRespRequest(clientInput)
        .map(
            request ->
                new RemoteCommand(
                    request, outputWriter, respCommandProcessor, respServiceProvider));
  }

  private Optional<RespRequest> createRespRequest(ImmutableList<ReplElement> clientInput) {
    String command = clientInput.get(0).getAsString();

    RespRequestCode requestCode;
    try {
      requestCode = RespRequestCode.valueOf(command.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }

    // TODO: improve error handling
    RespRequest respRequest =
        switch (requestCode) {
          case PING -> new RespRequest.PingRequest();
          case GET -> new RespRequest.GetRequest(clientInput.get(1).getAsString());
          case SET -> new RespRequest.SetRequest(
              clientInput.get(1).getAsString(), clientInput.get(2).getAsString());
          case DELETE -> new RespRequest.DeleteRequest(clientInput.get(1).getAsString());
        };

    return Optional.of(respRequest);
  }

  private ClientCommand createLocalCommand(ImmutableList<ReplElement> clientInput) {
    String command = clientInput.get(0).getAsString();

    if (LocalCommand.Help.commandStringMatches(command)) {
      return new LocalCommand.Help(outputWriter);
    } else if (LocalCommand.Exit.commandStringMatches(command)) {
      return new LocalCommand.Exit();
    } else {
      return new LocalCommand.Unknown(outputWriter, command);
    }
  }
}
