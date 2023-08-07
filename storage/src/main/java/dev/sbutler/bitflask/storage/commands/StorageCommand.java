package dev.sbutler.bitflask.storage.commands;

public interface StorageCommand {

  /** Executes the corresponding command returning the result. */
  StorageCommandResults execute();
}
