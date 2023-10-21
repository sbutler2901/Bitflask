package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import jakarta.inject.Inject;

/**
 * Handles interpreting command messages, processing server specific commands or dispatching storage
 * related commands to the StorageService for processing.
 */
public final class CommandProcessingService {

  private final ServerCommandFactory serverCommandFactory;

  @Inject
  CommandProcessingService(ServerCommandFactory serverCommandFactory) {
    this.serverCommandFactory = serverCommandFactory;
  }

  /**
   * Initiates processing of the provided message providing a ListenableFuture for retrieving the
   * results.
   */
  public ClientCommandResults processRespRequest(RespRequest request) {
    ServerCommand command = serverCommandFactory.createCommand(request);
    return command.execute();
  }
}
