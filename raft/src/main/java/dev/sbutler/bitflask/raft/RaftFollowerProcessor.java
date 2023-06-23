package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;

final class RaftFollowerProcessor implements RaftModeProcessor {

  private final RaftModeManager raftModeManager;
  private final RaftElectionTimer raftElectionTimer;

  @Inject
  RaftFollowerProcessor(RaftModeManager raftModeManager, RaftElectionTimer raftElectionTimer) {
    this.raftModeManager = raftModeManager;
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
