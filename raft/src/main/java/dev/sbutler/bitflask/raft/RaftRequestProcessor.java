package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;

/** Handles processing an incoming Raft RPC request based on the state of the server. */
final class RaftRequestProcessor {

  private final RaftStateManager raftStateManager;

  @Inject
  RaftRequestProcessor(RaftStateManager raftStateManager) {
    this.raftStateManager = raftStateManager;
  }

  RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }
}
