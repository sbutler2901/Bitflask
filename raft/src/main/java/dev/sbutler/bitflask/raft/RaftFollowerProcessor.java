package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;

final class RaftFollowerProcessor extends RaftModeProcessorBase {

  private final RaftElectionTimer raftElectionTimer;

  @Inject
  RaftFollowerProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftElectionTimer raftElectionTimer) {
    super(raftModeManager, raftPersistentState);
    this.raftElectionTimer = raftElectionTimer;
  }

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    raftElectionTimer.restart();
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    raftElectionTimer.restart();
    return AppendEntriesResponse.getDefaultInstance();
  }

  @Override
  public void run() {}

  @Override
  public void handleElectionTimeout() {
    raftModeManager.transitionToCandidateState();
  }
}
