package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageResponse;

final class ClientReadCommand implements ClientCommand {

  private final StorageCommandDTO.ReadDTO readDTO;

  ClientReadCommand(StorageCommandDTO.ReadDTO readDTO) {
    this.readDTO = readDTO;
  }

  @Override
  public StorageResponse execute() {
    return null;
  }
}
