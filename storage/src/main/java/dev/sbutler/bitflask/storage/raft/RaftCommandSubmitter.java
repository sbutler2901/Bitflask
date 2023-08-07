package dev.sbutler.bitflask.storage.raft;

import com.google.common.util.concurrent.ListenableFuture;

/** Supports committing {@link RaftCommand}s. */
public interface RaftCommandSubmitter {

  /**
   * A non-blocking call that submits a {@link RaftCommand} for replications.
   *
   * <p>If this Raft instance is the current leader, {@link RaftSubmitResults.Success} will be
   * returned. If not {@link RaftSubmitResults.NotCurrentLeader} will be returned.
   *
   * <p>If a command is successfully submitted and replicated any registered {@link
   * RaftCommandObserver}s will be notified with the provided RaftCommand.
   */
  RaftSubmitResults submitCommand(RaftCommand raftCommand);

  /** The results of submitting a {@link RaftCommand}. */
  sealed interface RaftSubmitResults {
    /**
     * Indicates the command was successfully submitted.
     *
     * @param submitFuture resolves once the command has been safely replicated and applied.
     */
    record Success(ListenableFuture<Void> submitFuture) implements RaftSubmitResults {}

    /**
     * Indicates this Raft instance is not the current Raft leader.
     *
     * <p>Returns the current leader's host and port for redirecting clients.
     */
    record NotCurrentLeader(RaftServerInfo currentLeaderInfo) implements RaftSubmitResults {}

    /** Indicates there is no known Raft leader. */
    record NoKnownLeader() implements RaftSubmitResults {}
  }
}
