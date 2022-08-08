package dev.sbutler.bitflask.storage.dispatcher;

public sealed interface StorageResponse {

  record Success(String message) implements StorageResponse {

  }

  record Failed(String message) implements StorageResponse {

  }
}
