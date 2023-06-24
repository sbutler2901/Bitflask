package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;

final class RaftCandidateProcessor implements RaftModeProcessor {

  private final RaftModeManager raftModeManager;
  private final RaftPersistentState raftPersistentState;
  private final RaftElectionTimer raftElectionTimer;

  @Inject
  RaftCandidateProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftElectionTimer raftElectionTimer) {
    this.raftModeManager = raftModeManager;
    this.raftPersistentState = raftPersistentState;
    this.raftElectionTimer = raftElectionTimer;
  }

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }

  public void handleElectionTimeout() {
    cancelCurrentElection();
    startNewElection();
  }

  @Override
  public void run() {
    startNewElection();
  }

  private void startNewElection() {
    raftPersistentState.incrementTermAndVoteForSelf();
    raftElectionTimer.restart();
    // 4. Send RequestVote RPCs to all other servers
  }

  private void cancelCurrentElection() {}
}
