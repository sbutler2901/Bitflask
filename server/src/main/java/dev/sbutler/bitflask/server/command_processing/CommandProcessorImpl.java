package dev.sbutler.bitflask.server.command_processing;

import dev.sbutler.bitflask.server.configuration.logging.InjectLogger;
import dev.sbutler.bitflask.storage.Storage;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.slf4j.Logger;

class CommandProcessorImpl implements CommandProcessor {

  private static final String PONG = "pong";
  private static final String READ_NOT_FOUND = "Value for key [%s] not found";
  private static final String READ_ERROR = "Error reading [%s]";
  private static final String WRITE_SUCCESS = "OK";
  private static final String WRITE_ERROR = "Error writing [%s] : [%s]";

  @InjectLogger
  Logger logger;

  private final Storage storage;

  @Inject
  CommandProcessorImpl(Storage storage) {
    this.storage = storage;
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
    Future<Optional<String>> readFuture = storage.read(key);
    try {
      return readFuture.get().orElse(String.format(READ_NOT_FOUND, key));
    } catch (InterruptedException e) {
      logger.error("Interrupted while waiting for response from Storage", e);
      return String.format(READ_ERROR, key);
    } catch (ExecutionException e) {
      logger.error("Read failed because of an error", e.getCause());
      return String.format(READ_ERROR, key);
    }
  }

  private String processSetCommand(ServerCommand setCommand) {
    String key = setCommand.args().get(0);
    String value = setCommand.args().get(1);
    Future<Void> writeFuture = storage.write(key, value);
    try {
      writeFuture.get();
    } catch (InterruptedException e) {
      logger.error("Interrupted while waiting for response from Storage", e);
      return String.format(WRITE_ERROR, key, value);
    } catch (ExecutionException e) {
      logger.error("Write failed because of an error", e.getCause());
      return String.format(WRITE_ERROR, key, value);
    }
    return WRITE_SUCCESS;
  }
}
