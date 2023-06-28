package dev.sbutler.bitflask.raft;

/** Captures all capabilities that a Raft mode processor must provide. */
sealed interface RaftModeProcessor extends RaftRpcHandler, RaftElectionTimeoutHandler, Runnable
    permits RaftModeProcessorBase {

  interface Factory {

    RaftFollowerProcessor createRaftFollowerProcessor();

    RaftCandidateProcessor createRaftCandidateProcessor();

    RaftLeaderProcessor createRaftLeaderProcessor();
  }
}
