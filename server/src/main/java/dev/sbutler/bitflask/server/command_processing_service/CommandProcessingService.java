package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
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
  public ListenableFuture<String> processCommandMessage(ImmutableList<String> commandMessage) {
    if (commandMessage.size() < 1) {
      return createFailureFuture("Message must contain at least one argument");
    }

    String messageCommand = commandMessage.get(0).trim();
    CommandType commandType;
    try {
      commandType = CommandType.valueOf(messageCommand.toUpperCase());
    } catch (IllegalArgumentException e) {
      return createFailureFuture(String.format("Invalid command [%s]", messageCommand));
    }

    ImmutableList<String> args = commandMessage.subList(1, commandMessage.size());
    try {
      ServerCommand command = commandFactory.createCommand(commandType, args);
      return command.execute();
    } catch (InvalidCommandArgumentsException e) {
      return createFailureFuture(e.getMessage());
    }
  }

  private static ListenableFuture<String> createFailureFuture(String responseMessage) {
    SettableFuture<String> failureFuture = SettableFuture.create();
    failureFuture.set(responseMessage);
    return failureFuture;
  }
}
