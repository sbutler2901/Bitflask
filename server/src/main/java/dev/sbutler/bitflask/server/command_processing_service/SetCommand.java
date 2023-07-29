package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageService;

/** Asynchronously submits a write request to the storage engine and processes the results. */
class SetCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageService storageService;
  private final String key;
  private final String value;

  public SetCommand(StorageService storageService, String key, String value) {
    this.storageService = storageService;
    this.key = key;
    this.value = value;
  }

  @Override
  public String execute() {
    StorageCommandDTO storageCommandDTO = new WriteDTO(key, value);
    try {
      return transformStorageResponse(storageService.processCommand(storageCommandDTO));
    } catch (Exception e) {
      logger.atWarning().withCause(e).log(
          "StorageService response threw an unexpected error while writing [%s]:[%s]", key, value);
      throw new StorageProcessingException(
          String.format("Unexpected failure writing [%s]:[%s]", key, value));
    }
  }

  private String transformStorageResponse(StorageResponse storageResponse) {
    return switch (storageResponse) {
      case StorageResponse.Success success -> success.message();
      case StorageResponse.Failed failed -> {
        logger.atWarning().log(
            "Storage failed writing [%s]:[%s]. %s", key, value, failed.message());
        yield String.format("Failed to write [%s]:[%s]", key, value);
      }
    };
  }
}
