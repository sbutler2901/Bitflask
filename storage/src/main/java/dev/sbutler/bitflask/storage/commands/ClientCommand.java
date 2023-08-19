package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.raft.Raft;

/** Command for executing the desired action against the storage engine. */
public final class ClientCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Raft raft;
  private final StorageCommand storageCommand;

  public ClientCommand(Raft raft, StorageCommand storageCommand) {
    this.raft = raft;
    this.storageCommand = storageCommand;
  }

  /** A blocking call that executes the corresponding command returning the results. */
  public ClientCommandResults execute() {
    StorageSubmitResults submitResults = raft.submitCommand(storageCommand);
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
    return switch (storageCommand) {
      case ReadCommand readCommand -> String.format(
          "Failed to read [%s]", readCommand.getDTO().key());
      case WriteCommand writeCommand -> String.format(
          "Failed to write [%s]:[%s]", writeCommand.getDTO().key(), writeCommand.getDTO().value());
      case DeleteCommand deleteCommand -> String.format(
          "Failed to delete [%s]", deleteCommand.getDTO().key());
    };
  }
}
