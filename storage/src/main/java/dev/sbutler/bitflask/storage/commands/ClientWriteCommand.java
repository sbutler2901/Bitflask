package dev.sbutler.bitflask.storage.commands;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;
import dev.sbutler.bitflask.storage.raft.RaftCommand;

/** Handles a client's request to write to storage. */
final class ClientWriteCommand implements ClientCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Raft raft;
  private final StorageCommandDTO.WriteDTO writeDTO;

  ClientWriteCommand(Raft raft, StorageCommandDTO.WriteDTO writeDTO) {
    this.raft = raft;
    this.writeDTO = writeDTO;
  }

  @Override
  public StorageSubmitResults execute() {
    RaftCommand.SetCommand command = new RaftCommand.SetCommand(writeDTO.key(), writeDTO.value());
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
      String message = String.format("Failed to write [%s]:%s]", writeDTO.key(), writeDTO.key());
      logger.atSevere().withCause(e).log(message);
      return new StorageSubmitResults.Success(immediateFuture(message));
    }
    return new StorageSubmitResults.Success(immediateFuture("OK"));
  }
}
