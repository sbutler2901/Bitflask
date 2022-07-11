package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.common.dispatcher.Dispatcher;
import dev.sbutler.bitflask.server.configuration.ServerCommandDispatcherCapacity;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServerCommandDispatcher extends Dispatcher<ServerCommand, ServerResponse> {

  @Inject
  ServerCommandDispatcher(@ServerCommandDispatcherCapacity int capacity) {
    super(capacity);
  }
}
