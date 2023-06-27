package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.RaftLog.LastLogEntryDetails;

/**
 * Base implementation of {@link RaftModeProcessor} providing some generic implementations of
 * required methods.
 */
abstract sealed class RaftModeProcessorBase implements RaftModeProcessor
    permits RaftFollowerProcessor, RaftCandidateProcessor, RaftLeaderProcessor {

  protected final RaftModeManager raftModeManager;
  protected final RaftPersistentState raftPersistentState;
  protected final RaftLog raftLog;

  RaftModeProcessorBase(
      RaftModeManager raftModeManager, RaftPersistentState raftPersistentState, RaftLog raftLog) {
    this.raftModeManager = raftModeManager;
    this.raftPersistentState = raftPersistentState;
    this.raftLog = raftLog;
  }

  /**
   * Used by subclasses to determine if a processor should update the server's current term and
   * convert to a follower, if not already one.
   *
   * <p>This method should be used when a term is received via a Raft RPC request or response.
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
  protected final void updateTermAndConvertToFollower(int rpcTerm) {
    raftPersistentState.setCurrentTermAndResetVote(rpcTerm);
    raftModeManager.transitionToFollowerState();
  }

  /**
   * The base Raft logic for handling a {@link RequestVoteRequest}.
   *
   * <p>If a processor must update the term and convert to a follower, this method should be called
   * afterward since a vote will need to be cast.
   */
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
      LastLogEntryDetails candidateLastLogEntryDetails =
          new LastLogEntryDetails(request.getLastLogTerm(), request.getLastLogIndex());
      LastLogEntryDetails localLastLogEntryDetails =
          raftPersistentState.getRaftLog().getLastLogDetails();
      boolean grantVote = candidateLastLogEntryDetails.compareTo(localLastLogEntryDetails) >= 0;
      if (grantVote) {
        raftPersistentState.setVotedForCandidateId(candidateRaftServerId);
      }
      response.setVoteGranted(grantVote);
    }

    return response.build();
  }

  /** The base Raft logic for handling a {@link AppendEntriesRequest}. */
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    AppendEntriesResponse.Builder response =
        AppendEntriesResponse.newBuilder().setTerm(raftPersistentState.getCurrentTerm());

    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      response.setSuccess(false);
    } else if (raftLog.logAtIndexHasMatchingTerm(
        request.getPrevLogIndex(), request.getPrevLogTerm())) {
      response.setSuccess(false);
    }

    return response.build();
  }
}
