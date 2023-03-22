package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import java.util.Optional;

/**
 * Handles submitting an asynchronous task to the storage engine for reading the value of a provided
 * key.
 */
final class ReadCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final ReadDTO readDTO;

  public ReadCommand(LSMTree lsmTree, ReadDTO readDTO) {
    this.lsmTree = lsmTree;
    this.readDTO = readDTO;
  }

  @Override
  public StorageResponse execute() {
    logger.atInfo().log("Submitting read for [%s]", readDTO.key());

    Optional<String> readValue;
    try {
      readValue = lsmTree.read(readDTO.key());
    } catch (StorageException e) {
      String responseErrorMessage = String.format("Failed to read [%s]", readDTO.key());
      logger.atWarning().withCause(e).log(responseErrorMessage);
      return new Failed(responseErrorMessage);
    }

    if (readValue.isEmpty()) {
      logger.atInfo().log("No value found for [%s]", readDTO.key());
      return new Success(String.format("[%s] not found", readDTO.key()));
    }

    logger.atInfo()
        .log("Found value for [%s]:[%s]", readDTO.key(), readValue.get());
    return new Success(readValue.get());
  }
}
