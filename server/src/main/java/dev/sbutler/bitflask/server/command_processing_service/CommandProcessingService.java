package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;

/**
 * Handles interpreting command messages, processing server specific commands or dispatching storage
 * related commands to the StorageService for processing.
 */
public final class CommandProcessingService {

  private final CommandFactory commandFactory;

  @Inject
  CommandProcessingService(CommandFactory commandFactory) {
    this.commandFactory = commandFactory;
  }

  /**
   * Initiates processing of the provided message providing a ListenableFuture for retrieving the
   * results.
   */
  public String processCommandMessage(ImmutableList<String> commandMessage) {
    if (commandMessage.isEmpty()) {
      return "Message must contain at least one argument";
    }

    String messageCommand = commandMessage.get(0).trim();
    CommandType commandType;
    try {
      commandType = CommandType.valueOf(messageCommand.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException(String.format("Invalid command [%s]", messageCommand));
    }

    ImmutableList<String> args = commandMessage.subList(1, commandMessage.size());
    return commandFactory.createCommand(commandType, args).execute();
  }
}
