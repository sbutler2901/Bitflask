package dev.sbutler.bitflask.storage.raft;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;

/** The interface for using the Raft Consensus protocol. */
@Singleton
public final class Raft implements RaftCommandSubmitter, RaftCommandSubjectRegistrar {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftModeManager raftModeManager;
  private final RaftCommandTopic raftCommandTopic;

  @Inject
  Raft(RaftModeManager raftModeManager, RaftCommandTopic raftCommandTopic) {
    this.raftModeManager = raftModeManager;
    this.raftCommandTopic = raftCommandTopic;
  }

  /** Returns true if this server is the current leader of the Raft cluster. */
  public boolean isCurrentLeader() {
    return raftModeManager.isCurrentLeader();
  }

  /** Returns the {@link RaftServerInfo} of the current cluster leader, if one is known. */
  public Optional<ServerConfig.ServerInfo> getCurrentLeaderServerInfo() {
    return raftModeManager.getCurrentLeaderServerInfo();
  }

  /** Submits a {@link RaftCommand} to be replicated. */
  public StorageSubmitResults submitCommand(StorageCommand storageCommand) {
    try {
      return raftModeManager.submitCommand(storageCommand);
    } catch (RaftException e) {
      logger.atSevere().withCause(e).log("Failed to submit command [%s]", storageCommand);
      return new StorageSubmitResults.Success(immediateFailedFuture(e));
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to submit command [%s]", storageCommand);
      return new StorageSubmitResults.Success(
          immediateFailedFuture(new RaftException("Unknown error while submitting.")));
    }
  }

  /**
   * Registers a {@link RaftCommandObserver} that will be called whenever a {@link RaftCommand} is
   * committed.
   */
  @Override
  public void register(RaftCommandObserver observer) {
    raftCommandTopic.register(observer);
  }

  /** Unregisters a {@link RaftCommandObserver}, if it was previously registered. */
  @Override
  public void unregister(RaftCommandObserver observer) {
    raftCommandTopic.unregister(observer);
  }
}
