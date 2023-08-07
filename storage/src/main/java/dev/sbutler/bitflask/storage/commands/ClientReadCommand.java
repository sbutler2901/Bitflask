package dev.sbutler.bitflask.storage.commands;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;

/** Handles a client's request to read from storage. */
final class ClientReadCommand implements ClientCommand {

  private final Raft raft;
  private final ReadCommand readCommand;

  ClientReadCommand(Raft raft, ReadCommand readCommand) {
    this.raft = raft;
    this.readCommand = readCommand;
  }

  @Override
  public StorageSubmitResults execute() {
    if (!raft.isCurrentLeader()) {
      raft.getCurrentLeaderServerInfo()
          .<StorageSubmitResults>map(StorageSubmitResults.NotCurrentLeader::new)
          .orElseGet(StorageSubmitResults.NoKnownLeader::new);
    }
    StorageCommandResults storageCommandResults = readCommand.execute();
    return switch (storageCommandResults) {
      case StorageCommandResults.Success success -> new StorageSubmitResults.Success(
          immediateFuture(success.message()));
      case StorageCommandResults.Failed failed -> new StorageSubmitResults.Success(
          immediateFuture(failed.message()));
    };
  }
}
