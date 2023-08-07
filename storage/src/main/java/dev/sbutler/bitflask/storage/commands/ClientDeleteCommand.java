package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.raft.Raft;
import dev.sbutler.bitflask.storage.raft.RaftCommand;
import dev.sbutler.bitflask.storage.raft.RaftCommandSubmitter;
import dev.sbutler.bitflask.storage.raft.RaftServerInfoConverter;

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
  public StorageResponse execute() {
    RaftCommand.DeleteCommand command = new RaftCommand.DeleteCommand(deleteDTO.key());
    RaftCommandSubmitter.RaftSubmitResults submitResults = raft.submitCommand(command);
    return switch (submitResults) {
      case RaftCommandSubmitter.RaftSubmitResults.Success success -> handleSuccess(success);
      case RaftCommandSubmitter.RaftSubmitResults.NotCurrentLeader
      notCurrentLeader -> new StorageResponse.NotCurrentLeader(
          RaftServerInfoConverter.INSTANCE.reverse().convert(notCurrentLeader.currentLeaderInfo()));
      case RaftCommandSubmitter.RaftSubmitResults.NoKnownLeader ignored -> new StorageResponse
          .NoKnownLeader();
    };
  }

  private StorageResponse handleSuccess(RaftCommandSubmitter.RaftSubmitResults.Success success) {
    try {
      success.submitFuture().get();
    } catch (Exception e) {
      String message = String.format("Failed to delete [%s]", deleteDTO.key());
      logger.atSevere().withCause(e).log(message);
      return new StorageResponse.Failed(message);
    }
    return new StorageResponse.Success("OK");
  }
}
