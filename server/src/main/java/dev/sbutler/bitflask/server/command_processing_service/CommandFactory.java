package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import jakarta.inject.Inject;
import java.util.List;

/** Handles creating {@link ServerCommand}s. */
final class CommandFactory {

  private final ListeningExecutorService listeningExecutorService;
  private final StorageCommandDispatcher storageCommandDispatcher;

  @Inject
  CommandFactory(
      ListeningExecutorService listeningExecutorService,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.listeningExecutorService = listeningExecutorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  /** */
  ServerCommand createCommand(CommandType commandType, ImmutableList<String> args) {
    if (!isValidCommandArgs(commandType, args)) {
      throw new InvalidCommandArgumentsException(
          String.format("Invalid arguments for command [%s]: %s", commandType, args));
    }

    return switch (commandType) {
      case PING -> createPingCommand();
      case GET -> createGetCommand(args.get(0));
      case SET -> createSetCommand(args.get(0), args.get(1));
      case DEL -> createDeleteCommand(args.get(0));
    };
  }

  private PingCommand createPingCommand() {
    return new PingCommand();
  }

  private GetCommand createGetCommand(String key) {
    return new GetCommand(listeningExecutorService, storageCommandDispatcher, key);
  }

  private SetCommand createSetCommand(String key, String value) {
    return new SetCommand(listeningExecutorService, storageCommandDispatcher, key, value);
  }

  private DeleteCommand createDeleteCommand(String key) {
    return new DeleteCommand(listeningExecutorService, storageCommandDispatcher, key);
  }

  private static boolean isValidCommandArgs(CommandType commandType, List<String> args) {
    return switch (commandType) {
      case PING -> args.size() == 0;
      case GET, DEL -> args.size() == 1;
      case SET -> args.size() == 2;
    };
  }
}
