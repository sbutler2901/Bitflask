package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.raft.RaftLog.LogEntryDetails;
import jakarta.inject.Provider;
import java.util.Optional;
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

  private volatile boolean shouldContinueExecuting = true;

  RaftModeProcessorBase(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState) {
    this.raftModeManager = raftModeManager;
    this.raftPersistentState = raftPersistentState;
    this.raftVolatileState = raftVolatileState;
  }

  final void terminateExecution() {
    shouldContinueExecuting = false;
  }

  final boolean shouldContinueExecuting() {
    return shouldContinueExecuting;
  }

  /**
   * A subclass can override this method to run custom logic before the current term is updated and
   * the server transitions to the Follower mode.
   *
   * <p>This method is executed anytime {@link #updateTermAndTransitionToFollower} or {@link
   * #updateTermAndTransitionToFollowerWithUnknownLeader} is called, before the term update and
   * transition has occurred.
   */
  protected void beforeUpdateTermAndTransitionToFollower(int rpcTerm) {}

  /** See {@link RaftModeProcessorBase#updateTermAndTransitionToFollower} */
  protected final void updateTermAndTransitionToFollowerWithUnknownLeader(int rpcTerm) {
    updateTermAndTransitionToFollower(rpcTerm, Optional.empty());
  }

  /**
   * Updates the term and, if this caller is not an instance of {@link RaftFollowerProcessor},
   * converts to a follower.
   */
  protected final void updateTermAndTransitionToFollower(
      int rpcTerm, Optional<RaftServerId> knownLeaderServerId) {
    beforeUpdateTermAndTransitionToFollower(rpcTerm);
    raftPersistentState.setCurrentTermAndResetVote(rpcTerm);
    raftModeManager.get().transitionToFollowerState(rpcTerm, knownLeaderServerId);
  }

  /**
   * Handles processing a {@link RequestVoteRequest} and responding with a {@link
   * RequestVoteResponse}.
   */
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      return RequestVoteResponse.newBuilder()
          .setTerm(raftPersistentState.getCurrentTerm())
          .setVoteGranted(false)
          .build();
    } else if (request.getTerm() > raftPersistentState.getCurrentTerm()) {
      getLogger()
          .atInfo()
          .log(
              "Transitioning to follower after receiving RequestVote with term [%d] and local term [%d].",
              request.getTerm(), raftPersistentState.getCurrentTerm());
      updateTermAndTransitionToFollowerWithUnknownLeader(request.getTerm());
    }

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
   * <p>This method will be executed after {@link #updateTermAndTransitionToFollower} or {@link
   * #updateTermAndTransitionToFollowerWithUnknownLeader}, if a term update and transition is
   * necessary.
   */
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {}

  /**
   * Handles processing a {@link AppendEntriesRequest} and responding with a {@link
   * AppendEntriesResponse}.
   */
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    if (request.getTerm() < raftPersistentState.getCurrentTerm()) {
      return AppendEntriesResponse.newBuilder()
          .setTerm(raftPersistentState.getCurrentTerm())
          .setSuccess(false)
          .build();
    } else if (request.getTerm() > raftPersistentState.getCurrentTerm()) {
      getLogger()
          .atInfo()
          .log(
              "Transitioning to follower after receiving AppendEntries with term [%d] and local term [%d].",
              request.getTerm(), raftPersistentState.getCurrentTerm());
      updateTermAndTransitionToFollower(
          request.getTerm(), Optional.of(new RaftServerId(request.getLeaderId())));
    }
    beforeProcessAppendEntriesRequest(request);

    raftVolatileState.setLeaderServerId(new RaftServerId(request.getLeaderId()));

    AppendEntriesResponse.Builder response =
        AppendEntriesResponse.newBuilder().setTerm(raftPersistentState.getCurrentTerm());

    if (request.getEntriesList().isEmpty()) {
      getLogger()
          .atInfo()
          .atMostEvery(10, TimeUnit.SECONDS)
          .log("Successfully processed heartbeat from server [%s].", request.getLeaderId());
      return response.setSuccess(true).build();
    }

    boolean appendSuccessful =
        raftPersistentState
            .getRaftLog()
            .appendEntriesAfterPrevEntry(
                new LogEntryDetails(request.getPrevLogTerm(), request.getPrevLogIndex()),
                request.getEntriesList());
    if (appendSuccessful
        && request.getLeaderCommit() > raftVolatileState.getHighestCommittedEntryIndex()) {
      raftVolatileState.setHighestCommittedEntryIndex(
          Math.min(
              request.getLeaderCommit(),
              raftPersistentState.getRaftLog().getLastLogEntryDetails().index()));
      getLogger().atInfo().log("Successfully appended [%d] entries.", request.getEntriesCount());
    } else {
      getLogger().atWarning().log("Failed to append [%d] entries.", request.getEntriesCount());
    }
    return response.setSuccess(appendSuccessful).build();
  }

  /**
   * Subclasses should override this to provide their logger instance so that logs in this class are
   * appropriately attributed to them.
   */
  protected abstract FluentLogger getLogger();
}
