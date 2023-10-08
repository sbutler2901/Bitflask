package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
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
  public ClientCommandResults processCommandMessage(ImmutableList<String> commandMessage) {
    if (commandMessage.isEmpty()) {
      throw new InvalidCommandException("Message must contain at least one argument");
    }

    ServerCommandType serverCommandType = getCommandType(commandMessage.get(0).trim());
    ImmutableList<String> args = commandMessage.subList(1, commandMessage.size());

    ServerCommand command = serverCommandFactory.createCommand(serverCommandType, args);
    return command.execute();
  }

  private ServerCommandType getCommandType(String messageCommand) {
    try {
      return ServerCommandType.valueOf(messageCommand.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException(String.format("Invalid command [%s]", messageCommand));
    }
  }
}
