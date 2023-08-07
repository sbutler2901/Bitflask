package dev.sbutler.bitflask.storage.commands;

/** The results of executing a {@link StorageCommand}. */
public sealed interface StorageCommandResults {

  record Success(String message) implements StorageCommandResults {}

  record Failed(String message) implements StorageCommandResults {}
}
