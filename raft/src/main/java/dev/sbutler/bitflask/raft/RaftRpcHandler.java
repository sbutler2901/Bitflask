package dev.sbutler.bitflask.raft;

/** Defines the methods required to handle Raft's RPC calls. */
interface RaftRpcHandler {

  /** Processes a {@link RequestVoteRequest} based on the current state of the server. */
  RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request);

  /** Processes a {@link AppendEntriesRequest} based on the current state of the server. */
  AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request);
}
