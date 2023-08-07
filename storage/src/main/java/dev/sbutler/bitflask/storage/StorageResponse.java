package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.config.ServerConfig;

public sealed interface StorageResponse {

  record Success(String message) implements StorageResponse {}

  record Failed(String message) implements StorageResponse {}

  record NotCurrentLeader(ServerConfig.ServerInfo currentLeaderInfo) implements StorageResponse {}

  record NoKnownLeader() implements StorageResponse {}
}
