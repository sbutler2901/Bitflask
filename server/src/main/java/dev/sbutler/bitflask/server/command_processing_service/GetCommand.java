package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import java.util.concurrent.ExecutorService;

/**
 * Asynchronously submits a read request to the storage engine and processes the results.
 */
class GetCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ExecutorService executorService;
  private final StorageCommandDispatcher storageCommandDispatcher;
  private final String key;

  public GetCommand(ExecutorService executorService,
      StorageCommandDispatcher storageCommandDispatcher,
      String key) {
    this.executorService = executorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
    this.key = key;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public ListenableFuture<String> execute() {
    StorageCommandDTO storageCommandDTO = new ReadDTO(key);
    ListenableFuture<StorageResponse> storageResponseFuture =
        storageCommandDispatcher.put(storageCommandDTO);

    return FluentFuture.from(storageResponseFuture)
        .transform(this::transformStorageResponse, executorService)
        .catching(Throwable.class, this::catchStorageFailure, executorService);
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

  private String catchStorageFailure(Throwable e) {
    logger.atWarning().withCause(e)
        .log("StorageService response threw an unexpected error while reading [%s]", key);
    return String.format("Unexpected failure getting [%s]", key);
  }
}
