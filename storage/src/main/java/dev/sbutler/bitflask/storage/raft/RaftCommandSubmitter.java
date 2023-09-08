package dev.sbutler.bitflask.storage.raft;

import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;

/** Supports committing {@link RaftCommand}s. */
public interface RaftCommandSubmitter {

  /**
   * A non-blocking call that submits a {@link StorageCommandDto} for replications.
   *
   * <p>If this Raft instance is the current leader, {@link StorageSubmitResults.Success} will be
   * returned. If not {@link StorageSubmitResults.NotCurrentLeader} will be returned.
   *
   * <p>If a StorageCommandDto is successfully submitted and replicated it will be converted into a
   * {@link dev.sbutler.bitflask.storage.commands.StorageCommand} and executed.
   */
  StorageSubmitResults submitCommand(StorageCommandDto storageCommandDto);
}
