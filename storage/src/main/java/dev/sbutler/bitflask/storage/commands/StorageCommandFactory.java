package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;

/**
 * Handles creating a {@link dev.sbutler.bitflask.storage.commands.StorageCommand} from a {@link
 * dev.sbutler.bitflask.storage.StorageCommandDTO}.
 */
public final class StorageCommandFactory {

  private final LSMTree lsmTree;

  @Inject
  StorageCommandFactory(LSMTree lsmTree) {
    this.lsmTree = lsmTree;
  }

  public StorageCommand createStorageCommand(StorageCommandDTO commandDTO) {
    return switch (commandDTO) {
      case StorageCommandDTO.ReadDTO readDTO -> new ReadCommand(lsmTree, readDTO);
      case StorageCommandDTO.WriteDTO writeDTO -> new WriteCommand(lsmTree, writeDTO);
      case StorageCommandDTO.DeleteDTO deleteDTO -> new DeleteCommand(lsmTree, deleteDTO);
    };
  }
}
