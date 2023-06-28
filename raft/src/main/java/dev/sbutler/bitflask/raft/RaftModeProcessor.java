package dev.sbutler.bitflask.raft;

/** Commons methods shared by all the various modes the Raft server can be operating in. */
sealed interface RaftModeProcessor extends RaftElectionTimeoutHandler, Runnable
    permits RaftModeProcessorBase {

  /** Processes a {@link RequestVoteRequest} based on the current state of the server. */
  RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request);

  /** Processes a {@link AppendEntriesRequest} based on the current state of the server. */
  AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request);

  interface Factory {

    RaftFollowerProcessor createRaftFollowerProcessor();

    RaftCandidateProcessor createRaftCandidateProcessor();

    RaftLeaderProcessor createRaftLeaderProcessor();
  }
}
