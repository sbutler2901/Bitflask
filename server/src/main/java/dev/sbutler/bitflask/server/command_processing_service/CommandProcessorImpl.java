package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;

class CommandProcessorImpl implements CommandProcessor {

  private static final String PONG = "pong";
  private static final String READ_NOT_FOUND = "Value for key [%s] not found";
  private static final String READ_ERROR = "Error reading [%s]";
  private static final String WRITE_SUCCESS = "OK";
  private static final String WRITE_ERROR = "Error writing [%s] : [%s]";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageService storageService;

  @Inject
  CommandProcessorImpl(StorageService storageService) {
    this.storageService = storageService;
  }

  public String processServerCommand(ServerCommand serverCommand) {
    Objects.requireNonNull(serverCommand);
    return switch (serverCommand.command()) {
      case GET -> processGetCommand(serverCommand);
      case SET -> processSetCommand(serverCommand);
      case PING -> PONG;
    };
  }

  private String processGetCommand(ServerCommand getCommand) {
    String key = getCommand.args().get(0);
    Future<Optional<String>> readFuture = storageService.read(key);
    try {
      return readFuture.get().orElse(String.format(READ_NOT_FOUND, key));
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e)
          .log("Interrupted while waiting for response from StorageService");
      return String.format(READ_ERROR, key);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e.getCause()).log("Read failed because of an error");
      return String.format(READ_ERROR, key);
    }
  }

  private String processSetCommand(ServerCommand setCommand) {
    String key = setCommand.args().get(0);
    String value = setCommand.args().get(1);
    Future<Void> writeFuture = storageService.write(key, value);
    try {
      writeFuture.get();
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e)
          .log("Interrupted while waiting for response from StorageService");
      return String.format(WRITE_ERROR, key, value);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e.getCause()).log("Write failed because of an error");
      return String.format(WRITE_ERROR, key, value);
    }
    return WRITE_SUCCESS;
  }
}
