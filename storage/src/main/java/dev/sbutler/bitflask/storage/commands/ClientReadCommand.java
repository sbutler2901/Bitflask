package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageResponse;
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
  public StorageResponse execute() {
    if (!raft.isCurrentLeader()) {
      raft.getCurrentLeaderServerInfo()
          .<StorageResponse>map(StorageResponse.NotCurrentLeader::new)
          .orElseGet(StorageResponse.NoKnownLeader::new);
    }
    return readCommand.execute();
  }
}
