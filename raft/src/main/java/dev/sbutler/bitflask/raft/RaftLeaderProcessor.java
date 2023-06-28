package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;

final class RaftLeaderProcessor extends RaftModeProcessorBase {

  @Inject
  RaftLeaderProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
  }

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }

  @Override
  public void handleElectionTimeout() {
    throw new IllegalStateException(
        "Raft in LEADER mode should not have an election timer running");
  }

  @Override
  public void run() {}
}
