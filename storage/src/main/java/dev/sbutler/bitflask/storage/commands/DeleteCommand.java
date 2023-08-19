package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Failed;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;

/**
 * Handles submitting an asynchronous task to the storage engine for deleting any mappings for the
 * provided key.
 */
final class DeleteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final DeleteDTO deleteDTO;

  public DeleteCommand(LSMTree lsmTree, DeleteDTO deleteDTO) {
    this.lsmTree = lsmTree;
    this.deleteDTO = deleteDTO;
  }

  @Override
  public StorageCommandResults execute() {
    logger.atInfo().log("Submitting delete for [%s]", deleteDTO.key());

    try {
      lsmTree.delete(deleteDTO.key());
    } catch (StorageException e) {
      String responseErrorMessage = String.format("Failed to delete [%s]", deleteDTO.key());
      logger.atWarning().withCause(e).log(responseErrorMessage);
      return new Failed(responseErrorMessage);
    }

    logger.atInfo().log("Successful delete of [%s]", deleteDTO.key());
    return new Success("OK");
  }

  @Override
  public DeleteDTO getDTO() {
    return deleteDTO;
  }
}
