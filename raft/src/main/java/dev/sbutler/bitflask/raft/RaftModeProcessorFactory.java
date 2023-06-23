package dev.sbutler.bitflask.raft;

interface RaftModeProcessorFactory {

  RaftFollowerProcessor createRaftFollowerProcessor();

  RaftCandidateProcessor createRaftCandidateProcessor();

  RaftLeaderProcessor createRaftLeaderProcessor();
}
