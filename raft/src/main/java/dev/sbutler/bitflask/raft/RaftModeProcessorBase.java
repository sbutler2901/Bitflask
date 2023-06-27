package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.RaftLog.LastLogDetails;

/**
 * Base implementation of {@link RaftModeProcessor} providing some generic implementations of
 * required methods.
 */
abstract sealed class RaftModeProcessorBase implements RaftModeProcessor
    permits RaftFollowerProcessor, RaftCandidateProcessor, RaftLeaderProcessor {

  protected final RaftModeManager raftModeManager;
  protected final RaftPersistentState raftPersistentState;

  RaftModeProcessorBase(RaftModeManager raftModeManager, RaftPersistentState raftPersistentState) {
    this.raftModeManager = raftModeManager;
    this.raftPersistentState = raftPersistentState;
  }

  /**
   * Used by subclasses to determine if a processor should update the server's current term and
   * convert to a follower, if not already one.
   *
   * <p>This should be used when a term is received via a Raft RPC request or response.
   */
  protected final boolean shouldUpdateTermAndConvertToFollower(long rpcTerm) {
    return rpcTerm > raftPersistentState.getCurrentTerm();
  }

  /**
   * Should be called by subclasses if {@link
   * RaftModeProcessorBase#shouldUpdateTermAndConvertToFollower(long)} is true.
   *
   * <p>This method should be used after a subclasses has executed its custom logic.
   */
  protected final void updateTermAndConvertToFollower(long rpcTerm) {
    raftPersistentState.setCurrentTermAndResetVote(rpcTerm);
    raftModeManager.transitionToFollowerState();
  }

  /** The base Raft logic for handling a {@link RequestVoteRequest}. */
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    RequestVoteResponse.Builder response =
        RequestVoteResponse.newBuilder().setTerm(raftPersistentState.getCurrentTerm());
    RaftServerId candidateRaftServerId = new RaftServerId(request.getCandidateId());
    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      response.setVoteGranted(false);
    } else if (raftPersistentState.getVotedForCandidateId().isPresent()
        && !raftPersistentState.getVotedForCandidateId().get().equals(candidateRaftServerId)) {
      response.setVoteGranted(false);
    } else {
      LastLogDetails candidateLastLogDetails =
          new LastLogDetails(request.getLastLogTerm(), request.getLastLogIndex());
      LastLogDetails localLastLogDetails = raftPersistentState.getRaftLog().getLastLogDetails();
      boolean grantVote = candidateLastLogDetails.compareTo(localLastLogDetails) >= 0;
      if (grantVote) {
        raftPersistentState.setVotedForCandidateId(candidateRaftServerId);
      }
      response.setVoteGranted(grantVote);
    }
    return response.build();
  }
}
