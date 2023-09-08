package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;

/** Handles creating a {@link StorageCommand} from a {@link StorageCommandDto}. */
final class StorageCommandFactory {

  private final LSMTree lsmTree;

  @Inject
  StorageCommandFactory(LSMTree lsmTree) {
    this.lsmTree = lsmTree;
  }

  StorageCommand create(StorageCommandDto commandDTO) {
    return switch (commandDTO) {
      case StorageCommandDto.ReadDto readDTO -> new ReadCommand(lsmTree, readDTO);
      case StorageCommandDto.WriteDto writeDTO -> new WriteCommand(lsmTree, writeDTO);
      case StorageCommandDto.DeleteDto deleteDTO -> new DeleteCommand(lsmTree, deleteDTO);
    };
  }
}
