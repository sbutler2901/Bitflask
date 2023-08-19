package dev.sbutler.bitflask.storage.raft;

import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommand;

/** Supports committing {@link RaftCommand}s. */
public interface RaftCommandSubmitter {

  /**
   * A non-blocking call that submits a {@link StorageCommand} for replications.
   *
   * <p>If this Raft instance is the current leader, {@link StorageSubmitResults.Success} will be
   * returned. If not {@link StorageSubmitResults.NotCurrentLeader} will be returned.
   *
   * <p>If a command is successfully submitted and replicated any registered {@link
   * RaftCommandObserver}s will be notified with the provided RaftCommand.
   */
  StorageSubmitResults submitCommand(StorageCommand raftCommand);
}
