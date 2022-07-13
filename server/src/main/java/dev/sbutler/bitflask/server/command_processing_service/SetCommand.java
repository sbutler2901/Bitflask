package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.StorageCommand;
import dev.sbutler.bitflask.storage.StorageCommand.Type;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.StorageResponse;
import java.util.concurrent.ExecutorService;

class SetCommand implements ServerCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ExecutorService executorService;
  private final StorageCommandDispatcher storageCommandDispatcher;
  private final String key;
  private final String value;

  public SetCommand(ExecutorService executorService,
      StorageCommandDispatcher storageCommandDispatcher,
      String key, String value) {
    this.executorService = executorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
    this.key = key;
    this.value = value;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public ListenableFuture<String> execute() {
    StorageCommand storageCommand = new StorageCommand(Type.WRITE, ImmutableList.of(key, value));
    ListenableFuture<StorageResponse> storageResponseFuture = storageCommandDispatcher.put(
        storageCommand);

    return FluentFuture.from(storageResponseFuture)
        .transform(this::transformStorageResponse, executorService)
        .catching(Throwable.class, this::catchStorageFailure, executorService);
  }

  private String transformStorageResponse(StorageResponse storageResponse) {
    return switch (storageResponse.status()) {
      case OK -> storageResponse.response().orElse("OK");
      case FAILED -> {
        logger.atWarning().log("Storage failed writing [%s]:[%s]. %s", key, value,
            storageResponse.errorMessage());
        yield String.format("Failed to write [%s]:[%s]", key, value);
      }
    };
  }

  private String catchStorageFailure(Throwable e) {
    logger.atWarning().withCause(e)
        .log("StorageService response threw an unexpected error while writing [%s]:[%s]", key,
            value);
    return String.format("Unexpected failure writing [%s]:[%s]", key, value);
  }
}
