package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;

/** Commands that handle interacting with the underlying storage engine. */
public sealed interface StorageCommand permits ReadCommand, WriteCommand, DeleteCommand {

  /** Executes the corresponding command returning the result. */
  StorageCommandResults execute();

  /** Returns the {@link dev.sbutler.bitflask.storage.StorageCommandDTO used for this command. */
  StorageCommandDTO getDTO();
}
