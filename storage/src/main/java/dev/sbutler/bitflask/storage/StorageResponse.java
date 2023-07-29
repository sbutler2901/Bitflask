package dev.sbutler.bitflask.storage;

public sealed interface StorageResponse {

  record Success(String message) implements StorageResponse {}

  record Failed(String message) implements StorageResponse {}
}
