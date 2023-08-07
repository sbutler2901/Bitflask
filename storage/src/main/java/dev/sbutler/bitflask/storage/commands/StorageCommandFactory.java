package dev.sbutler.bitflask.storage.commands;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import dev.sbutler.bitflask.storage.raft.RaftCommand;
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

  WriteCommand createWriteCommand(RaftCommand.SetCommand setCommand) {
    StorageCommandDTO.WriteDTO writeDTO =
        new StorageCommandDTO.WriteDTO(setCommand.key(), setCommand.value());
    return createWriteCommand(writeDTO);
  }

  WriteCommand createWriteCommand(StorageCommandDTO.WriteDTO writeDTO) {
    return new WriteCommand(lsmTree, writeDTO);
  }

  DeleteCommand createDeleteCommand(RaftCommand.DeleteCommand deleteCommand) {
    StorageCommandDTO.DeleteDTO deleteDTO = new StorageCommandDTO.DeleteDTO(deleteCommand.key());
    return createDeleteCommand(deleteDTO);
  }

  DeleteCommand createDeleteCommand(StorageCommandDTO.DeleteDTO deleteDTO) {
    return new DeleteCommand(lsmTree, deleteDTO);
  }
}
