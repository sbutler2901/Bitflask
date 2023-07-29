package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageResponse;

public interface StorageCommand {

  /** Executes the corresponding command returning the result. */
  StorageResponse execute();
}
