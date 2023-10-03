package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.raft.RaftLog.LogEntryDetails;
import jakarta.inject.Provider;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of {@link RaftModeProcessor} providing some generic implementations of
 * required methods.
 */
abstract sealed class RaftModeProcessorBase implements RaftModeProcessor
    permits RaftFollowerProcessor, RaftCandidateProcessor, RaftLeaderProcessor {

  protected final Provider<RaftModeManager> raftModeManager;
  protected final RaftPersistentState raftPersistentState;
  protected final RaftVolatileState raftVolatileState;

  RaftModeProcessorBase(
      Provider<RaftModeManager> raftModeManager,
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
   * Updates the term and, if this caller is not an instance of {@link RaftFollowerProcessor},
   * converts to a follower.
   *
   * <p>Subclasses should call this method if {@link
   * RaftModeProcessorBase#shouldUpdateTermAndTransitionToFollower(long)} is true for the term in
   * any {@link RequestVoteResponse} or {@link AppendEntriesResponse} received.
   */
  protected final void updateTermAndTransitionToFollower(int rpcTerm) {
    beforeUpdateTermAndTransitionToFollower(rpcTerm);
    raftPersistentState.setCurrentTermAndResetVote(rpcTerm);
    if (!RaftMode.FOLLOWER.equals(this.getRaftMode())) {
      raftModeManager.get().transitionToFollowerState();
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

  /**
   * A subclass can override this method to run custom logic before a {@link RequestVoteRequest} is
   * processed and {@link RequestVoteResponse} sent.
   *
   * <p>This method will be executed after {@link #updateTermAndTransitionToFollower(int)}, if a
   * term update and transition is necessary.
   */
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {}

  /**
   * A subclass can override this method to run custom logic before a {@link RequestVoteResponse} is
   * sent.
   */
  protected void afterProcessRequestVoteRequest(RequestVoteRequest request, boolean voteGranted) {}

  /**
   * Handles processing a {@link RequestVoteRequest} and responding with a {@link
   * RequestVoteResponse}.
   */
  public final RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    if (shouldUpdateTermAndTransitionToFollower(request.getTerm())) {
      // TODO: handle response when before transitioning
      getLogger()
          .atInfo()
          .log(
              "Transitioning to follower after receiving RequestVote with term [%d] and local term [%d].",
              request.getTerm(), raftPersistentState.getCurrentTerm());
      updateTermAndTransitionToFollower(request.getTerm());
    }
    beforeProcessRequestVoteRequest(request);

    RaftServerId candidateRaftServerId = new RaftServerId(request.getCandidateId());
    boolean voteGranted = shouldGrantVote(request, candidateRaftServerId);
    if (voteGranted) {
      raftPersistentState.setVotedForCandidateId(candidateRaftServerId);
      getLogger()
          .atInfo()
          .log(
              "Granting vote for server [%s] with term [%d] and local term [%d].",
              candidateRaftServerId.id(), request.getTerm(), raftPersistentState.getCurrentTerm());
    } else {
      getLogger()
          .atInfo()
          .log(
              "Deny vote for server [%s] with term [%d] and local term [%d]",
              candidateRaftServerId.id(), request.getTerm(), raftPersistentState.getCurrentTerm());
    }
    afterProcessRequestVoteRequest(request, voteGranted);
    return RequestVoteResponse.newBuilder()
        .setTerm(raftPersistentState.getCurrentTerm())
        .setVoteGranted(voteGranted)
        .build();
  }

  private boolean shouldGrantVote(RequestVoteRequest request, RaftServerId candidateRaftServerId) {
    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      return false;
    }
    return raftPersistentState
            .getVotedForCandidateId()
            .map(vote -> vote.equals(candidateRaftServerId))
            .orElse(true)
        && candidateLogUpToDate(request);
  }

  private boolean candidateLogUpToDate(RequestVoteRequest request) {
    LogEntryDetails candidateLastLogEntryDetails =
        new LogEntryDetails(request.getLastLogTerm(), request.getLastLogIndex());
    LogEntryDetails localLastLogEntryDetails =
        raftPersistentState.getRaftLog().getLastLogEntryDetails();
    return candidateLastLogEntryDetails.compareTo(localLastLogEntryDetails) >= 0;
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
   * A subclass can override this method to run custom logic before a {@link AppendEntriesResponse}
   * sent.
   */
  protected void afterProcessAppendEntriesRequest(AppendEntriesRequest request) {}

  /**
   * Handles processing a {@link AppendEntriesRequest} and responding with a {@link
   * AppendEntriesResponse}.
   */
  public final AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    if (shouldUpdateTermAndTransitionToFollower(request.getTerm())) {
      // TODO: handle response when before transitioning
      getLogger()
          .atInfo()
          .log(
              "Transitioning to follower after receiving AppendEntries with term [%d] and local term [%d].",
              request.getTerm(), raftPersistentState.getCurrentTerm());
      updateTermAndTransitionToFollower(request.getTerm());
    }
    beforeProcessAppendEntriesRequest(request);

    AppendEntriesResponse.Builder response =
        AppendEntriesResponse.newBuilder().setTerm(raftPersistentState.getCurrentTerm());

    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      getLogger()
          .atInfo()
          .atMostEvery(5, TimeUnit.SECONDS)
          .log(
              "Fail AppendEntries request with term [%d] and local term [%d].",
              request.getTerm(), raftPersistentState.getCurrentTerm());
      afterProcessAppendEntriesRequest(request);
      return response.setSuccess(false).build();
    }

    if (request.getEntriesList().isEmpty()) {
      getLogger()
          .atInfo()
          .atMostEvery(10, TimeUnit.SECONDS)
          .log("Successfully processed heartbeat from server [%s].", request.getLeaderId());
      afterProcessAppendEntriesRequest(request);
      return response.setSuccess(true).build();
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
                request.getLeaderCommit(),
                raftPersistentState.getRaftLog().getLastLogEntryDetails().index()));
      }
      getLogger().atInfo().log("Successfully appended [%d] entries.", request.getEntriesCount());
    } else {
      getLogger().atWarning().log("Failed to append [%d] entries.", request.getEntriesCount());
    }
    afterProcessAppendEntriesRequest(request);
    return response.setSuccess(appendSuccessful).build();
  }

  /**
   * Subclasses should override this to provide their logger instance so that logs in this class are
   * appropriately attributed to them.
   */
  protected abstract FluentLogger getLogger();
}
