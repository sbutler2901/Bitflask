package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.config.ServerConfig;

/** The results of executing a {@link ClientCommand}. */
public sealed interface ClientCommandResults {
  /** Contains a client friendly message for the successful execution of a {@link ClientCommand}. */
  record Success(String message) implements ClientCommandResults {}

  /** Contains a client friendly message for the failed execution of a {@link ClientCommand}. */
  record Failure(String message) implements ClientCommandResults {}

  /**
   * Indicates this storage server instance is not the current leader.
   *
   * <p>Contains the current leader's {@link dev.sbutler.bitflask.config.ServerConfig.ServerInfo}.
   */
  record NotCurrentLeader(ServerConfig.ServerInfo currentLeaderInfo)
      implements ClientCommandResults {}

  /** Indicates there is no known leader of storage servers. */
  record NoKnownLeader() implements ClientCommandResults {}
}
