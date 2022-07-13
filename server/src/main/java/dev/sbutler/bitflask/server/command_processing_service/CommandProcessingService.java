package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
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
    if (commandType == null) {
      return false;
    }
    return switch (commandType) {
      case GET -> args != null && args.size() == 1;
      case SET -> args != null && args.size() == 2;
      case PING -> args == null || args.size() == 0;
    };
  }
}
