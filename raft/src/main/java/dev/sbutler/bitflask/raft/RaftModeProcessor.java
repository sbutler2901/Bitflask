package dev.sbutler.bitflask.raft;

/** Commons methods shared by all the various modes the Raft server can be operating in. */
sealed interface RaftModeProcessor extends HandlesElectionTimeout, Runnable
    permits RaftFollowerProcessor, RaftCandidateProcessor, RaftLeaderProcessor {

  /** Processes a {@link RequestVoteRequest} based on the current state of the server. */
  RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request);

  /** Processes a {@link AppendEntriesRequest} based on the current state of the server. */
  AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request);
}
