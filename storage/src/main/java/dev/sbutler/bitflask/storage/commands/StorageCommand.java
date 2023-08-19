package dev.sbutler.bitflask.storage.commands;

/** Commands that handle interacting with the underlying storage engine. */
public sealed interface StorageCommand permits ReadCommand, WriteCommand, DeleteCommand {

  /** Executes the corresponding command returning the result. */
  StorageCommandResults execute();

  /** Returns the {@link StorageCommandDTO used for this command. */
  StorageCommandDTO getDTO();
}
