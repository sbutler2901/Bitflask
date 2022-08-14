package dev.sbutler.bitflask.storage.dispatcher;

import dev.sbutler.bitflask.common.dispatcher.Dispatcher;

public class StorageCommandDispatcher extends Dispatcher<StorageCommandDTO, StorageResponse> {

  public StorageCommandDispatcher(int capacity) {
    super(capacity);
  }
}
