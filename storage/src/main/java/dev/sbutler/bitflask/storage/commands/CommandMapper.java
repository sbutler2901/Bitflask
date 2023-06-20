package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;

/** Maps incoming {@link StorageCommandDTO}s into executable {@link StorageCommand}s. */
public class CommandMapper {

  private final LSMTree lsmTree;

  @Inject
  public CommandMapper(LSMTree lsmTree) {
    this.lsmTree = lsmTree;
  }

  public StorageCommand mapToCommand(StorageCommandDTO commandDTO) {
    return switch (commandDTO) {
      case ReadDTO readDTO -> new ReadCommand(lsmTree, readDTO);
      case WriteDTO writeDTO -> new WriteCommand(lsmTree, writeDTO);
      case DeleteDTO deleteDTO -> new DeleteCommand(lsmTree, deleteDTO);
    };
  }
}
