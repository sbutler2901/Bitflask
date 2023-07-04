package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;

/** The interface for using the Raft Consensus protocol. */
@Singleton
public final class Raft implements RaftCommandSubmitter, RaftCommandSubjectRegistrar {

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
  public Optional<RaftServerInfo> getCurrentLeaderServerInfo() {
    return raftModeManager.getCurrentLeaderServerInfo();
  }

  /** Submits a {@link RaftCommand} to be replicated. */
  public RaftSubmitResults submitCommand(RaftCommand raftCommand) {
    return raftModeManager.submitCommand(raftCommand);
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
