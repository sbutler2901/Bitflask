package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Handles interpreting command messages, processing server specific commands or dispatching storage
 * related commands to the StorageService for processing.
 */
public class CommandProcessingService {

  private final ExecutorService executorService;
  private final StorageCommandDispatcher storageCommandDispatcher;

  @Inject
  CommandProcessingService(ExecutorService executorService,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.executorService = executorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  /**
   * Initiates processing of the provided message providing a ListenableFuture for retrieving the
   * results.
   */
  public ListenableFuture<String> processCommandMessage(ImmutableList<String> commandMessage) {
    checkNotNull(commandMessage);
    if (commandMessage.size() < 1) {
      SettableFuture<String> failureFuture = SettableFuture.create();
      failureFuture.set("Message must contain at least one argument");
      return failureFuture;
    }

    String messageCommand = commandMessage.get(0).trim();
    CommandType commandType = CommandType.valueOf(messageCommand.toUpperCase());
    ImmutableList<String> args = commandMessage.subList(1, commandMessage.size());
    if (!isValidCommandArgs(commandType, args)) {
      SettableFuture<String> failureFuture = SettableFuture.create();
      failureFuture.set(String.format("Invalid arguments for [%s]: %s", messageCommand, args));
      return failureFuture;
    }

    ServerCommand command = createCommand(commandType, args);
    return command.execute();
  }

  private ServerCommand createCommand(CommandType commandType, ImmutableList<String> args) {
    return switch (commandType) {
      case PING -> new PingCommand();
      case GET -> new GetCommand(executorService, storageCommandDispatcher, args.get(0));
      case SET ->
          new SetCommand(executorService, storageCommandDispatcher, args.get(0), args.get(1));
      case DEL -> new DeleteCommand(executorService, storageCommandDispatcher, args.get(0));
    };
  }

  private static boolean isValidCommandArgs(CommandType commandType, List<String> args) {
    return switch (commandType) {
      case PING -> args.size() == 0;
      case GET, DEL -> args.size() == 1;
      case SET -> args.size() == 2;
    };
  }
}
