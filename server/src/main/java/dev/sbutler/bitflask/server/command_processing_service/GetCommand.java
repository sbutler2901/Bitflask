package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageService;

/** Asynchronously submits a read request to the storage engine and processes the results. */
class GetCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageService storageService;
  private final String key;

  public GetCommand(StorageService storageService, String key) {
    this.storageService = storageService;
    this.key = key;
  }

  @Override
  public String execute() {
    StorageCommandDTO storageCommandDTO = new ReadDTO(key);
    try {
      return transformStorageResponse(storageService.processCommand(storageCommandDTO));
    } catch (Exception e) {
      logger.atWarning().withCause(e).log(
          "StorageService response threw an unexpected error while reading [%s]", key);
      return String.format("Unexpected failure getting [%s]", key);
    }
  }

  private String transformStorageResponse(StorageResponse storageResponse) {
    return switch (storageResponse) {
      case StorageResponse.Success success -> success.message();
      case StorageResponse.Failed failed -> {
        logger.atWarning().log("Storage failed reading [%s]: %s", key, failed.message());
        yield String.format("Failed to read [%s]", key);
      }
    };
  }
}
