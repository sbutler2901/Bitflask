package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageSubmitResults;

public interface ClientCommand {
  /** A blocking call that executes the corresponding command returning the results. */
  StorageSubmitResults execute();
}
