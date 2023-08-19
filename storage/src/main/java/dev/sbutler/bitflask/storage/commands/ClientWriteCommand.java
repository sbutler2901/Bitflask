package dev.sbutler.bitflask.storage.commands;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;

/** Handles a client's request to write to storage. */
final class ClientWriteCommand implements ClientCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Raft raft;
  private final WriteCommand writeCommand;

  ClientWriteCommand(Raft raft, WriteCommand writeCommand) {
    this.raft = raft;
    this.writeCommand = writeCommand;
  }

  @Override
  public StorageSubmitResults execute() {
    StorageSubmitResults submitResults = raft.submitCommand(writeCommand);
    if (submitResults instanceof StorageSubmitResults.Success successResults) {
      return handleSuccess(successResults);
    }
    return submitResults;
  }

  private StorageSubmitResults handleSuccess(StorageSubmitResults.Success success) {
    try {
      success.submitFuture().get();
    } catch (Exception e) {
      String message =
          String.format(
              "Failed to write [%s]:[%s]",
              writeCommand.getDTO().key(), writeCommand.getDTO().value());
      logger.atSevere().withCause(e).log(message);
      return new StorageSubmitResults.Success(immediateFuture(message));
    }
    return new StorageSubmitResults.Success(immediateFuture("OK"));
  }
}
