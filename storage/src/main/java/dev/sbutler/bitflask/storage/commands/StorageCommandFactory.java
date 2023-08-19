package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;

/** Handles creating a {@link StorageCommand} from a {@link StorageCommandDTO}. */
final class StorageCommandFactory {

  private final LSMTree lsmTree;

  @Inject
  StorageCommandFactory(LSMTree lsmTree) {
    this.lsmTree = lsmTree;
  }

  StorageCommand createStorageCommand(StorageCommandDTO commandDTO) {
    return switch (commandDTO) {
      case StorageCommandDTO.ReadDTO readDTO -> new ReadCommand(lsmTree, readDTO);
      case StorageCommandDTO.WriteDTO writeDTO -> new WriteCommand(lsmTree, writeDTO);
      case StorageCommandDTO.DeleteDTO deleteDTO -> new DeleteCommand(lsmTree, deleteDTO);
    };
  }
}
