package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageResponse;

final class ClientDeleteCommand implements ClientCommand {

  private final StorageCommandDTO.DeleteDTO deleteDTO;

  ClientDeleteCommand(StorageCommandDTO.DeleteDTO deleteDTO) {
    this.deleteDTO = deleteDTO;
  }

  @Override
  public StorageResponse execute() {
    return null;
  }
}
