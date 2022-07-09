package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.StorageCommand;
import dev.sbutler.bitflask.storage.StorageCommand.Type;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.StorageResponse;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class CommandProcessingService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageCommandDispatcher storageCommandDispatcher;

  @Inject
  CommandProcessingService(StorageCommandDispatcher storageCommandDispatcher) {
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  public String processServerCommand(ServerCommand serverCommand) {
    Objects.requireNonNull(serverCommand);
    return switch (serverCommand.command()) {
      case GET -> processGetCommand(serverCommand);
      case SET -> processSetCommand(serverCommand);
      case PING -> "pong";
    };
  }

  private String processGetCommand(ServerCommand getCommand) {
    // TODO: connect futures together rather than blocking this service
    String key = getCommand.args().get(0);
    ListenableFuture<StorageResponse> readResponse = storageCommandDispatcher.put(
        new StorageCommand(Type.READ, ImmutableList.of(key)));
    try {
      return readResponse.get().response()
          .orElse(String.format("Value for key [%s] not found", key));
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e)
          .log("Interrupted while waiting for response from StorageService");
      return String.format("Error reading [%s]", key);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e.getCause()).log("Read failed because of an error");
      return String.format("Error reading [%s]", key);
    }
  }

  private String processSetCommand(ServerCommand setCommand) {
    // TODO: connect futures together rather than blocking this service
    String key = setCommand.args().get(0);
    String value = setCommand.args().get(1);
    ListenableFuture<StorageResponse> writeResponse = storageCommandDispatcher.put(
        new StorageCommand(Type.WRITE, ImmutableList.of(key, value)));
    try {
      writeResponse.get();
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e)
          .log("Interrupted while waiting for response from StorageService");
      return String.format("Error writing [%s] : [%s]", key, value);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e.getCause()).log("Write failed because of an error");
      return String.format("Error writing [%s] : [%s]", key, value);
    }
    return "OK";
  }
}
