package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;

/**
 * Handles the {@link RaftModeManager.RaftMode#FOLLOWER} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the
 * follower mode.
 */
final class RaftFollowerProcessor extends RaftModeProcessorBase {

  private final RaftElectionTimer raftElectionTimer;

  @Inject
  RaftFollowerProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftElectionTimer raftElectionTimer) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.raftElectionTimer = raftElectionTimer;
  }

  @Override
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {
    raftElectionTimer.restart();
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    raftElectionTimer.restart();
  }

  @Override
  public void handleElectionTimeout() {
    raftModeManager.transitionToCandidateState();
  }

  @Override
  public void run() {
    // Nothing for follower to do besides process incoming RPCs
  }
}
