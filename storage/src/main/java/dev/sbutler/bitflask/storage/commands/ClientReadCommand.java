package dev.sbutler.bitflask.storage.commands;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;

/** Handles a client's request to read from storage. */
final class ClientReadCommand implements ClientCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Raft raft;
  private final ReadCommand readCommand;

  ClientReadCommand(Raft raft, ReadCommand readCommand) {
    this.raft = raft;
    this.readCommand = readCommand;
  }

  @Override
  public StorageSubmitResults execute() {
    StorageSubmitResults submitResults = raft.submitCommand(readCommand);
    if (submitResults instanceof StorageSubmitResults.Success successResults) {
      return handleSuccess(successResults);
    }
    return submitResults;
  }

  private StorageSubmitResults handleSuccess(StorageSubmitResults.Success success) {
    try {
      success.submitFuture().get();
    } catch (Exception e) {
      String message = String.format("Failed to read [%s]", readCommand.getDTO().key());
      logger.atSevere().withCause(e).log(message);
      return new StorageSubmitResults.Success(immediateFuture(message));
    }
    return new StorageSubmitResults.Success(immediateFuture("OK"));
  }
}
