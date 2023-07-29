package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageService;

/** Submits a blocking delete request to the storage engine and processes the results. */
public class DeleteCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageService storageService;
  private final String key;

  public DeleteCommand(StorageService storageService, String key) {
    this.storageService = storageService;
    this.key = key;
  }

  @Override
  public String execute() {
    StorageCommandDTO storageCommandDTO = new DeleteDTO(key);
    try {
      return transformStorageResponse(storageService.processCommand(storageCommandDTO));
    } catch (Exception e) {
      logger.atWarning().withCause(e).log(
          "StorageService response threw an unexpected error while deleting [%s]", key);
      throw new StorageProcessingException(String.format("Unexpected failure deleting [%s]", key));
    }
  }

  private String transformStorageResponse(StorageResponse storageResponse) {
    return switch (storageResponse) {
      case StorageResponse.Success success -> success.message();
      case StorageResponse.Failed failed -> {
        logger.atWarning().log("Storage failed deleting [%s]: %s", key, failed.message());
        yield String.format("Failed to delete [%s]", key);
      }
    };
  }
}
