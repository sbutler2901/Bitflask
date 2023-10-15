package dev.sbutler.bitflask.storage.raft;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** The interface for using the Raft Consensus protocol. */
@Singleton
public final class Raft implements RaftCommandSubmitter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftModeManager raftModeManager;

  @Inject
  Raft(RaftModeManager raftModeManager) {
    this.raftModeManager = raftModeManager;
  }

  /** Submits a {@link RaftCommand} to be replicated. */
  public StorageSubmitResults submitCommand(StorageCommandDto storageCommandDto) {
    try {
      return raftModeManager.submitCommand(storageCommandDto);
    } catch (RaftException e) {
      logger.atSevere().withCause(e).log("Failed to submit command [%s]", storageCommandDto);
      return new StorageSubmitResults.Success(immediateFailedFuture(e));
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to submit command [%s]", storageCommandDto);
      return new StorageSubmitResults.Success(
          immediateFailedFuture(new RaftException("Unknown error while submitting.", e)));
    }
  }
}
