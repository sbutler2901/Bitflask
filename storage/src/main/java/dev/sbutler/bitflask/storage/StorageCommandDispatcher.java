package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.common.dispatcher.Dispatcher;

public class StorageCommandDispatcher extends Dispatcher<StorageCommand, StorageResponse> {

  StorageCommandDispatcher(int capacity) {
    super(capacity);
  }
}
