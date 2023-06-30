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
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    raftElectionTimer.restart();
    if (shouldUpdateTermAndConvertToFollower(request.getTerm())) {
      updateTermAndConvertToFollower(request.getTerm());
    }
    return super.processRequestVoteRequest(request);
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    raftElectionTimer.restart();
    if (shouldUpdateTermAndConvertToFollower(request.getTerm())) {
      updateTermAndConvertToFollower(request.getTerm());
    }
    return super.processAppendEntriesRequest(request);
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
