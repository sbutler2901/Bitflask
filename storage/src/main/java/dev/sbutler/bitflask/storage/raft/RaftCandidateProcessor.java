package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.raft.RaftCandidateRpcClient.RequestVotesResults;
import dev.sbutler.bitflask.storage.raft.RaftLog.LogEntryDetails;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Handles the {@link RaftMode#CANDIDATE} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the
 * Candidate mode.
 */
public final class RaftCandidateProcessor extends RaftModeProcessorBase {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final RaftMode RAFT_MODE = RaftMode.CANDIDATE;

  private final RaftConfiguration raftConfiguration;
  private final RaftElectionTimer raftElectionTimer;
  private final RaftClusterRpcChannelManager raftClusterRpcChannelManager;

  private volatile boolean shouldContinueElections = true;
  private volatile boolean hasElectionTimeoutOccurred = false;

  @Inject
  RaftCandidateProcessor(
      RaftConfiguration raftConfiguration,
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftElectionTimer raftElectionTimer,
      RaftClusterRpcChannelManager raftClusterRpcChannelManager) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.raftConfiguration = raftConfiguration;
    this.raftElectionTimer = raftElectionTimer;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
  }

  @Override
  public RaftMode getRaftMode() {
    return RAFT_MODE;
  }

  @Override
  protected void beforeUpdateTermAndTransitionToFollower(int rpcTerm) {
    shouldContinueElections = false;
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    // Concede to new leader
    if (request.getTerm() >= raftPersistentState.getCurrentTerm()) {
      shouldContinueElections = false;
      updateTermAndTransitionToFollower(request.getTerm());
    }
  }

  public void handleElectionTimeout() {
    logger.atInfo().log("Handling election timeout.");
    hasElectionTimeoutOccurred = true;
  }

  /**
   * The main loop of this class that executes until the RequestVotes RPCs sent cause termination,
   * or an RPC is received from another server that causes termination.
   */
  @Override
  public void run() {
    while (shouldContinueElections) {
      startNewElection();
      // Do not start another election until election timer timeout
      while (shouldContinueElections && !hasElectionTimeoutOccurred) {
        Thread.onSpinWait();
      }
    }
  }

  /** Starts and handles a single election cycle. */
  private void startNewElection() {
    raftPersistentState.incrementTermAndVoteForSelf();
    int timerDelay = raftElectionTimer.restart();
    logger.atInfo().log(
        "Started new election term [%d] with election timer delay [%dms].",
        raftPersistentState.getCurrentTerm(), timerDelay);
    hasElectionTimeoutOccurred = false;
    try (var rpcClient = raftClusterRpcChannelManager.createRaftCandidateRpcClient()) {
      sendRequestVotesAndWait(rpcClient);
    }
  }

  /**
   * Requests a vote from all other Raft servers in the cluster and waits.
   *
   * <p>This method waits until
   *
   * <ul>
   *   <li>The election has been won.
   *   <li>All responses have been received.
   *   <li>A greater term is encountered in a {@link RequestVoteResponse}.
   *   <li>An election timeout has occurred.
   *   <li>The candidate is halted.
   */
  private void sendRequestVotesAndWait(RaftCandidateRpcClient rpcClient) {
    LogEntryDetails lastLogEntryDetails = raftPersistentState.getRaftLog().getLastLogEntryDetails();
    RequestVoteRequest request =
        RequestVoteRequest.newBuilder()
            .setCandidateId(raftConfiguration.thisRaftServerId().id())
            .setTerm(raftPersistentState.getCurrentTerm())
            .setLastLogIndex(lastLogEntryDetails.index())
            .setLastLogTerm(lastLogEntryDetails.term())
            .build();
    rpcClient.requestVotes(request);

    while (shouldContinueElections && !hasElectionTimeoutOccurred) {
      RequestVotesResults requestVotesResults = rpcClient.getCurrentRequestVotesResults();
      if (shouldUpdateTermAndTransitionToFollower(requestVotesResults.largestTermSeen())) {
        logger.atInfo().log(
            "Found larger term [%d]. Transitioning to Follower.",
            requestVotesResults.largestTermSeen());
        updateTermAndTransitionToFollower(requestVotesResults.largestTermSeen());
      } else if (requestVotesResults.receivedMajorityVotes()) {
        shouldContinueElections = false;
        logger.atInfo().log("Received majority of votes. Transitioning to Leader");
        raftModeManager.get().transitionToLeaderState();
      } else if (requestVotesResults.allResponsesReceived()) {
        logger.atInfo().log(
            "All responses for term [%d] received without verdict. Waiting to start next election.",
            request.getTerm());
        break;
      }
    }
  }

  @Override
  protected FluentLogger getLogger() {
    return logger;
  }
}
