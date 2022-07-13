package dev.sbutler.bitflask.server.command_processing_service.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.StorageCommand;
import dev.sbutler.bitflask.storage.StorageCommand.Type;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.StorageResponse;
import java.util.concurrent.ExecutorService;

public class GetCommand implements ServerCommand {

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
    StorageCommand storageCommand = new StorageCommand(Type.READ, ImmutableList.of(key));
    ListenableFuture<StorageResponse> storageResponseFuture = storageCommandDispatcher.put(
        storageCommand);

    return FluentFuture.from(storageResponseFuture)
        .transform(this::transformStorageResponse, executorService)
        .catching(Throwable.class, this::catchStorageFailure, executorService);
  }

  private String transformStorageResponse(StorageResponse storageResponse) {
    return switch (storageResponse.status()) {
      case OK -> storageResponse.response().orElse(String.format("[%s] not found", key));
      case FAILED -> {
        logger.atWarning().log("Storage failed reading [%s]: %s", key,
            storageResponse.errorMessage());
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
