package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import javax.inject.Inject;

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

  PingCommand createPingCommand() {
    return new PingCommand();

  }

  GetCommand createGetCommand(String key) {
    return new GetCommand(listeningExecutorService, storageCommandDispatcher, key);
  }

  SetCommand createSetCommand(String key, String value) {
    return new SetCommand(listeningExecutorService, storageCommandDispatcher, key, value);
  }

  DeleteCommand createDeleteCommand(String key) {
    return new DeleteCommand(listeningExecutorService, storageCommandDispatcher, key);
  }
}
