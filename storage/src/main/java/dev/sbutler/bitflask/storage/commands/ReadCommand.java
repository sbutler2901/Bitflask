package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.StorageResponse.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import java.util.Optional;

/** Handles submitting a read of the provided key to the storage engine. */
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

    logger.atInfo().log("Found value for [%s]:[%s]", readDTO.key(), readValue.get());
    return new Success(readValue.get());
  }
}
