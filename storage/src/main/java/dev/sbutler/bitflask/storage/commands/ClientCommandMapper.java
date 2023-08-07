package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import jakarta.inject.Inject;

public class ClientCommandMapper {
  @Inject
  ClientCommandMapper() {}

  public ClientCommand mapToClientCommand(StorageCommandDTO storageCommandDTO) {
    return switch (storageCommandDTO) {
      case StorageCommandDTO.ReadDTO readDTO -> new ClientReadCommand(readDTO);
      case StorageCommandDTO.WriteDTO writeDTO -> new ClientWriteCommand(writeDTO);
      case StorageCommandDTO.DeleteDTO deleteDTO -> new ClientDeleteCommand(deleteDTO);
    };
  }
}
