package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.raft.Raft;
import dev.sbutler.bitflask.storage.raft.RaftCommand;
import dev.sbutler.bitflask.storage.raft.RaftCommandSubmitter;
import dev.sbutler.bitflask.storage.raft.RaftServerInfoConverter;

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
  public StorageResponse execute() {
    RaftCommand.SetCommand command = new RaftCommand.SetCommand(writeDTO.key(), writeDTO.value());
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
      String message = String.format("Failed to write [%s]:%s]", writeDTO.key(), writeDTO.key());
      logger.atSevere().withCause(e).log();
      return new StorageResponse.Failed(message);
    }
    return new StorageResponse.Success("OK");
  }
}
