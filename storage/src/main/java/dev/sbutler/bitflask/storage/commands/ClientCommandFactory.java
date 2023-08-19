package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.raft.Raft;
import jakarta.inject.Inject;

/** Factory for creating {@link dev.sbutler.bitflask.storage.commands.ClientCommand}s. */
public class ClientCommandFactory {

  private final Raft raft;
  private final StorageCommandFactory storageCommandFactory;

  @Inject
  ClientCommandFactory(Raft raft, StorageCommandFactory storageCommandFactory) {
    this.raft = raft;
    this.storageCommandFactory = storageCommandFactory;
  }

  public ClientCommand create(StorageCommandDTO commandDTO) {
    StorageCommand storageCommand = storageCommandFactory.create(commandDTO);
    return new ClientCommand(raft, storageCommand);
  }
}
