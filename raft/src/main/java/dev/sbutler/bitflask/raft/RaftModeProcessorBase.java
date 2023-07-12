package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.RaftLog.LogEntryDetails;

/**
 * Base implementation of {@link RaftModeProcessor} providing some generic implementations of
 * required methods.
 */
abstract sealed class RaftModeProcessorBase implements RaftModeProcessor
    permits RaftFollowerProcessor, RaftCandidateProcessor, RaftLeaderProcessor {

  protected final RaftModeManager raftModeManager;
  protected final RaftPersistentState raftPersistentState;
  protected final RaftVolatileState raftVolatileState;

  RaftModeProcessorBase(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState) {
    this.raftModeManager = raftModeManager;
    this.raftPersistentState = raftPersistentState;
    this.raftVolatileState = raftVolatileState;
  }

  /**
   * Used to determine if a processor should update the server's current term and convert to a
   * follower, if not already one.
   *
   * <p>Subclasses should call this method with the term in any {@link RequestVoteResponse} or
   * {@link AppendEntriesResponse} received.
   */
  protected final boolean shouldUpdateTermAndTransitionToFollower(long rpcTerm) {
    return rpcTerm > raftPersistentState.getCurrentTerm();
  }

  /**
   * A subclass can override this method to run custom logic before the current term is updated and
   * the server transitions to the Follower mode.
   *
   * <p>This method is executed anytime {@link #updateTermAndTransitionToFollower(int)} is called,
   * before the term update and transition has occurred.
   */
  protected void beforeUpdateTermAndTransitionToFollower(int rpcTerm) {}

  /**
   * Updates the term and, if this caller is not an instance of {@link RaftLeaderProcessor},
   * converts to a follower.
   *
   * <p>Subclasses should call this method if {@link
   * RaftModeProcessorBase#shouldUpdateTermAndTransitionToFollower(long)} is true for the term in
   * any {@link RequestVoteResponse} or {@link AppendEntriesResponse} received.
   */
  protected final void updateTermAndTransitionToFollower(int rpcTerm) {
    beforeUpdateTermAndTransitionToFollower(rpcTerm);
    raftPersistentState.setCurrentTermAndResetVote(rpcTerm);
    if (!this.getClass().isInstance(RaftFollowerProcessor.class)) {
      raftModeManager.transitionToFollowerState();
    }
  }

  @Override
  public boolean commitCommand(SetCommand setCommand) {
    return false;
  }

  @Override
  public boolean commitCommand(DeleteCommand deleteCommand) {
    return false;
  }

  private void checkRequestRpcTerm(int rpcTerm) {
    if (shouldUpdateTermAndTransitionToFollower(rpcTerm)) {
      updateTermAndTransitionToFollower(rpcTerm);
    }
  }

  /**
   * A subclass can override this method to run custom logic before a {@link RequestVoteRequest} is
   * processed and {@link RequestVoteResponse} sent.
   *
   * <p>This method will be executed after {@link #updateTermAndTransitionToFollower(int)}, if a
   * term update and transition is necessary.
   */
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {}

  /**
   * Handles processing a {@link RequestVoteRequest} and responding with a {@link
   * RequestVoteResponse}.
   */
  public final RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    checkRequestRpcTerm(request.getTerm());
    beforeProcessRequestVoteRequest(request);

    RequestVoteResponse.Builder response =
        RequestVoteResponse.newBuilder().setTerm(raftPersistentState.getCurrentTerm());
    RaftServerId candidateRaftServerId = new RaftServerId(request.getCandidateId());

    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      response.setVoteGranted(false);
    } else if (raftPersistentState.getVotedForCandidateId().isPresent()
        && !raftPersistentState.getVotedForCandidateId().get().equals(candidateRaftServerId)) {
      response.setVoteGranted(false);
    } else {
      LogEntryDetails candidateLastLogEntryDetails =
          new LogEntryDetails(request.getLastLogTerm(), request.getLastLogIndex());
      LogEntryDetails localLastLogEntryDetails =
          raftPersistentState.getRaftLog().getLastLogEntryDetails();
      boolean grantVote = candidateLastLogEntryDetails.compareTo(localLastLogEntryDetails) >= 0;
      if (grantVote) {
        raftPersistentState.setVotedForCandidateId(candidateRaftServerId);
      }
      response.setVoteGranted(grantVote);
    }

    return response.build();
  }

  /**
   * A subclass can override this method to run custom logic before a {@link AppendEntriesRequest}
   * is processed and {@link AppendEntriesResponse} sent.
   *
   * <p>This method will be executed after {@link #updateTermAndTransitionToFollower(int)}, if a
   * term update and transition is necessary.
   */
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {}

  /**
   * Handles processing a {@link AppendEntriesRequest} and responding with a {@link
   * AppendEntriesResponse}.
   */
  public final AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    checkRequestRpcTerm(request.getTerm());
    beforeProcessAppendEntriesRequest(request);

    AppendEntriesResponse.Builder response =
        AppendEntriesResponse.newBuilder().setTerm(raftPersistentState.getCurrentTerm());

    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      response.setSuccess(false);
      return response.build();
    }

    boolean appendSuccessful =
        raftPersistentState
            .getRaftLog()
            .appendEntriesAfterPrevEntry(
                new LogEntryDetails(request.getPrevLogTerm(), request.getPrevLogIndex()),
                request.getEntriesList());
    if (appendSuccessful) {
      raftVolatileState.setLeaderId(new RaftServerId(request.getLeaderId()));
      if (request.getLeaderCommit() > raftVolatileState.getHighestCommittedEntryIndex()) {
        raftVolatileState.setHighestCommittedEntryIndex(
            Math.min(
                request.getLeaderCommit(), raftPersistentState.getRaftLog().getLastEntryIndex()));
        // TODO: apply any newly committed entries
      }
    }
    response.setSuccess(appendSuccessful);
    return response.build();
  }
}
