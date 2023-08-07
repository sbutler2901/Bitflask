package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.raft.Raft;
import jakarta.inject.Inject;

public class ClientCommandMapper {

  private final Raft raft;
  private final StorageCommandFactory storageCommandFactory;

  @Inject
  ClientCommandMapper(Raft raft, StorageCommandFactory storageCommandFactory) {
    this.raft = raft;
    this.storageCommandFactory = storageCommandFactory;
  }

  public ClientCommand mapToCommand(StorageCommandDTO storageCommandDTO) {
    return switch (storageCommandDTO) {
      case StorageCommandDTO.ReadDTO readDTO -> new ClientReadCommand(
          raft, storageCommandFactory.createReadCommand(readDTO));
      case StorageCommandDTO.WriteDTO writeDTO -> new ClientWriteCommand(writeDTO);
      case StorageCommandDTO.DeleteDTO deleteDTO -> new ClientDeleteCommand(deleteDTO);
    };
  }
}
