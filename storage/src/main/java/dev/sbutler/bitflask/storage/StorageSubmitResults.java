package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.config.ServerConfig;

/** The results of submitting a command to Storage. */
public sealed interface StorageSubmitResults {

  /**
   * Indicates the command was successfully submitted.
   *
   * @param submitFuture resolves once the command has been safely replicated and applied.
   */
  record Success(ListenableFuture<Void> submitFuture) implements StorageSubmitResults {}

  /**
   * Indicates this storage server instance is not the current leader.
   *
   * <p>Returns the current leader's {@link dev.sbutler.bitflask.config.ServerConfig.ServerInfo}.
   */
  record NotCurrentLeader(ServerConfig.ServerInfo currentLeaderInfo)
      implements StorageSubmitResults {}

  /** Indicates there is no known leader of storage servers. */
  record NoKnownLeader() implements StorageSubmitResults {}
}
