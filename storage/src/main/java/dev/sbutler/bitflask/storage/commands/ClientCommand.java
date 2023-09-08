package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;
import jakarta.inject.Inject;

/** Command for executing the desired action against the storage engine. */
public final class ClientCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Raft raft;
  private final StorageCommandDTO storageCommandDto;

  public ClientCommand(Raft raft, StorageCommandDTO storageCommandDto) {
    this.raft = raft;
    this.storageCommandDto = storageCommandDto;
  }

  public static class Factory {
    private final Raft raft;

    @Inject
    Factory(Raft raft) {
      this.raft = raft;
    }

    public ClientCommand create(StorageCommandDTO commandDTO) {
      return new ClientCommand(raft, commandDTO);
    }
  }

  /** A blocking call that executes the corresponding command returning the results. */
  public ClientCommandResults execute() {
    //    StorageSubmitResults submitResults = raft.submitCommand(storageCommandDto);
    // TODO: update raft to accept dto
    StorageSubmitResults submitResults = new StorageSubmitResults.NoKnownLeader();
    return switch (submitResults) {
      case StorageSubmitResults.Success success -> handleSuccessfulSubmission(success);
      case StorageSubmitResults.NotCurrentLeader notCurrentLeader -> new ClientCommandResults
          .NotCurrentLeader(notCurrentLeader.currentLeaderInfo());
      case StorageSubmitResults.NoKnownLeader noKnownLeader -> new ClientCommandResults
          .NoKnownLeader();
    };
  }

  private ClientCommandResults handleSuccessfulSubmission(StorageSubmitResults.Success success) {
    try {
      success.submitFuture().get();
    } catch (Exception e) {
      String failureMessage = getFailureMessage();
      logger.atSevere().withCause(e).log(failureMessage);
      return new ClientCommandResults.Failure(failureMessage);
    }
    return new ClientCommandResults.Success("OK");
  }

  /** Returns a client friendly message when there is a failure submitting to storage. */
  private String getFailureMessage() {
    return switch (storageCommandDto) {
      case StorageCommandDTO.ReadDTO dto -> String.format("Failed to read [%s]", dto.key());
      case StorageCommandDTO.WriteDTO dto -> String.format(
          "Failed to write [%s]:[%s]", dto.key(), dto.value());
      case StorageCommandDTO.DeleteDTO deleteDTO -> String.format(
          "Failed to delete [%s]", deleteDTO.key());
    };
  }
}
