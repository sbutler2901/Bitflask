package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.common.dispatcher.Dispatcher;
import dev.sbutler.bitflask.storage.configuration.StorageDispatcherCapacity;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StorageCommandDispatcher extends Dispatcher<StorageCommand, StorageResponse> {

  @Inject
  StorageCommandDispatcher(@StorageDispatcherCapacity int capacity) {
    super(capacity);
  }
}
