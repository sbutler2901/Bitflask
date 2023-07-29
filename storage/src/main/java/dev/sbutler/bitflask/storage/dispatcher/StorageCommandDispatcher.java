package dev.sbutler.bitflask.storage.dispatcher;

import dev.sbutler.bitflask.common.dispatcher.Dispatcher;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageResponse;

public class StorageCommandDispatcher extends Dispatcher<StorageCommandDTO, StorageResponse> {

  public StorageCommandDispatcher(int capacity) {
    super(capacity);
  }
}
