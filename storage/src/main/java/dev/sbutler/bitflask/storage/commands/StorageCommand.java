package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;

public interface StorageCommand {

  /**
   * Executes the corresponding command returning the result.
   */
  StorageResponse execute();
}
