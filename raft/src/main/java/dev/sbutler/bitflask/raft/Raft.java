package dev.sbutler.bitflask.raft;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.raft.exceptions.RaftException;
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
    return raftModeManager
        .getCurrentLeaderServerInfo()
        .map(raftServerInfo -> RaftServerInfoConverter.INSTANCE.reverse().convert(raftServerInfo));
  }

  /** Submits a {@link RaftCommand} to be replicated. */
  public RaftSubmitResults submitCommand(RaftCommand raftCommand) {
    try {
      return raftModeManager.submitCommand(raftCommand);
    } catch (RaftException e) {
      logger.atSevere().withCause(e).log("Failed to submit command [%s]", raftCommand);
      return new RaftSubmitResults.Success(immediateFailedFuture(e));
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to submit command [%s]", raftCommand);
      return new RaftSubmitResults.Success(
          immediateFailedFuture(new RaftException("Unknown error while submitting.")));
    }
  }

  /**
   * Registers a {@link dev.sbutler.bitflask.raft.RaftCommandObserver} that will be called whenever
   * a {@link dev.sbutler.bitflask.raft.RaftCommand} is committed.
   */
  @Override
  public void register(RaftCommandObserver observer) {
    raftCommandTopic.register(observer);
  }

  /**
   * Unregisters a {@link dev.sbutler.bitflask.raft.RaftCommandObserver}, if it was previously
   * registered.
   */
  @Override
  public void unregister(RaftCommandObserver observer) {
    raftCommandTopic.unregister(observer);
  }
}
