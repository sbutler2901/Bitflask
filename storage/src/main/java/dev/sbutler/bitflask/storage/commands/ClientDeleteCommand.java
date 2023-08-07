package dev.sbutler.bitflask.storage.commands;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;
import dev.sbutler.bitflask.storage.raft.RaftCommand;

/** Handles a client's request to delete to storage. */
final class ClientDeleteCommand implements ClientCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Raft raft;
  private final StorageCommandDTO.DeleteDTO deleteDTO;

  ClientDeleteCommand(Raft raft, StorageCommandDTO.DeleteDTO deleteDTO) {
    this.raft = raft;
    this.deleteDTO = deleteDTO;
  }

  @Override
  public StorageSubmitResults execute() {
    RaftCommand.DeleteCommand command = new RaftCommand.DeleteCommand(deleteDTO.key());
    StorageSubmitResults submitResults = raft.submitCommand(command);
    if (submitResults instanceof StorageSubmitResults.Success successResults) {
      return handleSuccess(successResults);
    }
    return submitResults;
  }

  private StorageSubmitResults handleSuccess(StorageSubmitResults.Success success) {
    try {
      success.submitFuture().get();
    } catch (Exception e) {
      String message = String.format("Failed to delete [%s]", deleteDTO.key());
      logger.atSevere().withCause(e).log(message);
      return new StorageSubmitResults.Success(immediateFuture(message));
    }
    return new StorageSubmitResults.Success(immediateFuture("OK"));
  }
}
