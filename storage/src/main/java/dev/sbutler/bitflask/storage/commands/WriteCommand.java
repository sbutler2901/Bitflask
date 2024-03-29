package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto.WriteDto;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Failed;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;

/**
 * Handles submitting an asynchronous task to the storage engine for writing a key:value mapping.
 */
final class WriteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final WriteDto writeDTO;

  public WriteCommand(LSMTree lsmTree, WriteDto writeDTO) {
    this.lsmTree = lsmTree;
    this.writeDTO = writeDTO;
  }

  @Override
  public StorageCommandResults execute() {
    logger.atInfo().log("Submitting write for [%s]:[%s]", writeDTO.key(), writeDTO.value());

    try {
      lsmTree.write(writeDTO.key(), writeDTO.value());
    } catch (StorageException e) {
      String responseErrorMessage =
          String.format("Failed to write [%s]:[%s]", writeDTO.key(), writeDTO.value());
      logger.atWarning().withCause(e).log(responseErrorMessage);
      return new Failed(responseErrorMessage);
    }

    logger.atInfo().log("Successful write of [%s]:[%s]", writeDTO.key(), writeDTO.value());
    return new Success("OK");
  }

  @Override
  public WriteDto getDTO() {
    return writeDTO;
  }
}
