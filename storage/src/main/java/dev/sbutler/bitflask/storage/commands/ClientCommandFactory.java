package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.raft.Raft;
import jakarta.inject.Inject;

public class ClientCommandFactory {

  private final Raft raft;
  private final StorageCommandFactory storageCommandFactory;

  @Inject
  ClientCommandFactory(Raft raft, StorageCommandFactory storageCommandFactory) {
    this.raft = raft;
    this.storageCommandFactory = storageCommandFactory;
  }

  public ClientCommand createClientCommand(StorageCommandDTO storageCommandDTO) {
    return switch (storageCommandDTO) {
      case StorageCommandDTO.ReadDTO readDTO -> new ClientReadCommand(
          raft, storageCommandFactory.createReadCommand(readDTO));
      case StorageCommandDTO.WriteDTO writeDTO -> new ClientWriteCommand(
          raft, storageCommandFactory.createWriteCommand(writeDTO));
      case StorageCommandDTO.DeleteDTO deleteDTO -> new ClientDeleteCommand(
          raft, storageCommandFactory.createDeleteCommand(deleteDTO));
    };
  }
}
