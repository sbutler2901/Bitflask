package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public class CommandProcessingService {

  private final ExecutorService executorService;
  private final StorageCommandDispatcher storageCommandDispatcher;

  @Inject
  CommandProcessingService(ExecutorService executorService,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.executorService = executorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  public ListenableFuture<String> processMessage(ImmutableList<String> message) {
    checkNotNull(message);
    checkArgument(message.size() >= 1, "Message must contain at least one argument");
    CommandType commandType = CommandType.valueOf(message.get(0).trim().toUpperCase());
    ImmutableList<String> args = message.subList(1, message.size());

    if (!isValidCommandArgs(commandType, args)) {
      SettableFuture<String> failureFuture = SettableFuture.create();
      failureFuture.setException(new IllegalArgumentException(
          "Invalid arguments for the commandType: " + commandType + ", " + args));
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
    };
  }

  private static boolean isValidCommandArgs(CommandType commandType, List<String> args) {
    return switch (commandType) {
      case PING -> args.size() == 0;
      case GET -> args.size() == 1;
      case SET -> args.size() == 2;
    };
  }
}
