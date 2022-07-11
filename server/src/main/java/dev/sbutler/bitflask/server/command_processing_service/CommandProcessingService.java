package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.server.command_processing_service.ServerResponse.Status;
import dev.sbutler.bitflask.storage.StorageCommand;
import dev.sbutler.bitflask.storage.StorageCommand.Type;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.StorageResponse;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CommandProcessingService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ExecutorService executorService;
  private final ServerCommandDispatcher serverCommandDispatcher;
  private final StorageCommandDispatcher storageCommandDispatcher;
  private volatile boolean isRunning = true;

  @Inject
  CommandProcessingService(ExecutorService executorService,
      ServerCommandDispatcher serverCommandDispatcher,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.executorService = executorService;
    this.serverCommandDispatcher = serverCommandDispatcher;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  @Override
  protected void run() {
    while (isRunning) {
      try {
        DispatcherSubmission<ServerCommand, ServerResponse> submission =
            serverCommandDispatcher.poll(100, TimeUnit.MILLISECONDS);
        if (submission != null) {
          processSubmission(submission);
        }
      } catch (InterruptedException e) {
        // Continue running until shutdown has been triggered
        logger.atWarning().withCause(e).log("Interrupted while polling dispatcher");
      }
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    System.out.println("CommandProcessingService shutdown triggered");
    isRunning = false;
    serverCommandDispatcher.closeAndDrain();
    System.out.println("CommandProcessingService shutdown");
  }

  private void processSubmission(DispatcherSubmission<ServerCommand, ServerResponse> submission) {
    ServerCommand command = submission.command();
    SettableFuture<ServerResponse> responseFuture = submission.responseFuture();
    switch (command.command()) {
      case GET -> responseFuture.setFuture(processGetCommand(command));
      case SET -> responseFuture.setFuture(processSetCommand(command));
      case PING -> responseFuture.set(processPong());
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private ListenableFuture<ServerResponse> processGetCommand(ServerCommand getCommand) {
    String key = getCommand.args().get(0);
    StorageCommand storageCommand = new StorageCommand(Type.READ, ImmutableList.of(key));
    ListenableFuture<StorageResponse> storageResponseFuture = storageCommandDispatcher.put(
        storageCommand);

    return FluentFuture.from(storageResponseFuture)
        .transform(storageResponse -> switch (storageResponse.status()) {
          case OK -> new ServerResponse(Status.OK, storageResponse.response(), Optional.empty());
          case FAILED ->
              new ServerResponse(Status.FAILED, Optional.empty(), storageResponse.errorMessage());
        }, executorService)
        .catching(Throwable.class, e -> {
          logger.atWarning().withCause(e)
              .log("StorageService response threw an unexpected error while reading [%s]", key);
          return new ServerResponse(Status.FAILED, Optional.empty(),
              Optional.of(String.format("Unexpected failure getting [%s]", key)));
        }, executorService);
  }

  @SuppressWarnings("UnstableApiUsage")
  private ListenableFuture<ServerResponse> processSetCommand(ServerCommand setCommand) {
    String key = setCommand.args().get(0);
    String value = setCommand.args().get(1);
    StorageCommand storageCommand = new StorageCommand(Type.WRITE, ImmutableList.of(key, value));
    ListenableFuture<StorageResponse> storageResponseFuture = storageCommandDispatcher.put(
        storageCommand);

    return FluentFuture.from(storageResponseFuture)
        .transform(storageResponse -> switch (storageResponse.status()) {
          case OK -> new ServerResponse(Status.OK, storageResponse.response(), Optional.empty());
          case FAILED ->
              new ServerResponse(Status.FAILED, Optional.empty(), storageResponse.errorMessage());
        }, executorService)
        .catching(Throwable.class, e -> {
          logger.atWarning().withCause(e)
              .log("Storage response threw an unexpected error while writing [%s]:[%s]", key,
                  value);
          return new ServerResponse(Status.FAILED, Optional.empty(),
              Optional.of(String.format("Unexpected failure setting [%s]:[%s]", key, value)));
        }, executorService);
  }

  private ServerResponse processPong() {
    return new ServerResponse(Status.OK, Optional.of("pong"), Optional.empty());
  }

}
