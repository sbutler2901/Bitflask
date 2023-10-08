package dev.sbutler.bitflask.storage.raft;

/** Captures all capabilities that a Raft mode processor must provide. */
sealed interface RaftModeProcessor extends RaftRpcHandler, Runnable permits RaftModeProcessorBase {

  /** Returns the {@link RaftMode} representing the current {@link RaftModeProcessor}. */
  RaftMode getRaftMode();

  interface Factory {

    RaftFollowerProcessor createRaftFollowerProcessor();

    RaftCandidateProcessor createRaftCandidateProcessor();

    RaftLeaderProcessor createRaftLeaderProcessor();
  }
}
