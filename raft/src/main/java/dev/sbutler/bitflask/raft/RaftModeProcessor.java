package dev.sbutler.bitflask.raft;

/** Captures all capabilities that a Raft mode processor must provide. */
sealed interface RaftModeProcessor extends RaftRpcHandler, RaftElectionTimeoutHandler, Runnable
    permits RaftModeProcessorBase {

  /**
   * Commits an entry with the provided {@link SetCommand}. Returns false if the server is not the
   * cluster's leader
   */
  boolean commitCommand(SetCommand setCommand);

  /**
   * Commits an entry with the provided {@link DeleteCommand}. Returns false if the server is not
   * the cluster's leader.
   */
  boolean commitCommand(DeleteCommand deleteCommand);

  interface Factory {

    RaftFollowerProcessor createRaftFollowerProcessor();

    RaftCandidateProcessor createRaftCandidateProcessor();

    RaftLeaderProcessor createRaftLeaderProcessor();
  }
}
