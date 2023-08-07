package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageResponse;

final class ClientWriteCommand implements ClientCommand {

  private final StorageCommandDTO.WriteDTO writeDTO;

  ClientWriteCommand(StorageCommandDTO.WriteDTO writeDTO) {
    this.writeDTO = writeDTO;
  }

  @Override
  public StorageResponse execute() {
    return null;
  }
}
