package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;

final class StorageCommandFactory {

  private final LSMTree lsmTree;

  @Inject
  StorageCommandFactory(LSMTree lsmTree) {
    this.lsmTree = lsmTree;
  }

  ReadCommand createReadCommand(StorageCommandDTO.ReadDTO readDTO) {
    return new ReadCommand(lsmTree, readDTO);
  }

  WriteCommand createWriteCommand(StorageCommandDTO.WriteDTO writeDTO) {
    return new WriteCommand(lsmTree, writeDTO);
  }

  DeleteCommand createDeleteCommand(StorageCommandDTO.DeleteDTO deleteDTO) {
    return new DeleteCommand(lsmTree, deleteDTO);
  }
}
