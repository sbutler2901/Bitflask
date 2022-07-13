package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.server.command_processing_service.commands.CommandType;
import dev.sbutler.bitflask.server.command_processing_service.commands.GetCommand;
import dev.sbutler.bitflask.server.command_processing_service.commands.PingCommand;
import dev.sbutler.bitflask.server.command_processing_service.commands.ServerCommand;
import dev.sbutler.bitflask.server.command_processing_service.commands.SetCommand;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
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

    if (!CommandType.isValidCommandArgs(commandType, args)) {
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
}
