package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.RaftClusterCandidateRpcClient.RequestVotesResults;
import dev.sbutler.bitflask.raft.RaftLog.LastLogDetails;
import jakarta.inject.Inject;

/**
 * Handles the {@link RaftModeManager.RaftMode#CANDIDATE} stage of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the
 * Candidate stage.
 */
final class RaftCandidateProcessor extends RaftModeProcessorBase {

  private final RaftClusterConfiguration raftClusterConfiguration;
  private final RaftModeManager raftModeManager;
  private final RaftPersistentState raftPersistentState;
  private final RaftElectionTimer raftElectionTimer;
  private final RaftClusterRpcChannelManager raftClusterRpcChannelManager;
  private final RaftLog raftLog;

  private volatile boolean shouldContinueElections = true;
  private volatile boolean hasElectionTimeoutOccurred = false;

  @Inject
  RaftCandidateProcessor(
      RaftClusterConfiguration raftClusterConfiguration,
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftElectionTimer raftElectionTimer,
      RaftClusterRpcChannelManager raftClusterRpcChannelManager,
      RaftLog raftLog) {
    this.raftClusterConfiguration = raftClusterConfiguration;
    this.raftModeManager = raftModeManager;
    this.raftPersistentState = raftPersistentState;
    this.raftElectionTimer = raftElectionTimer;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
    this.raftLog = raftLog;
  }

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }

  public void handleElectionTimeout() {
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
    raftElectionTimer.restart();
    hasElectionTimeoutOccurred = false;
    try (var candidateRpcClient =
        raftClusterRpcChannelManager.createRaftClusterCandidateRpcClient()) {
      sendRequestVotesAndWait(candidateRpcClient);
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
  private void sendRequestVotesAndWait(RaftClusterCandidateRpcClient candidateRpcClient) {
    LastLogDetails lastLogDetails = raftLog.getLastLogDetails();
    RequestVoteRequest request =
        RequestVoteRequest.newBuilder()
            .setCandidateId(raftClusterConfiguration.thisRaftServerId().id())
            .setTerm(raftPersistentState.getCurrentTerm())
            .setLastLogIndex(lastLogDetails.index())
            .setLastLogTerm(lastLogDetails.term())
            .build();
    candidateRpcClient.requestVotes(request);

    while (shouldContinueElections && !hasElectionTimeoutOccurred) {
      RequestVotesResults requestVotesResults = candidateRpcClient.getCurrentRequestVotesResults();
      if (requestVotesResults.largestTermSeen() > raftPersistentState.getCurrentTerm()) {
        handleGreaterTermEncountered(requestVotesResults.largestTermSeen());
      }
      if (receivedMajorityVotes(requestVotesResults)) {
        handleWonElection();
      }
      if (requestVotesResults.allResponsesReceived()) {
        break;
      }
    }
  }

  private void handleWonElection() {
    haltCandidate();
    raftModeManager.transitionToLeaderState();
  }

  private void handleAnotherLeaderEncountered() {
    haltCandidate();
    raftModeManager.transitionToFollowerState();
  }

  private void handleGreaterTermEncountered(long greaterTerm) {
    haltCandidate();
    raftPersistentState.setCurrentTermAndResetVote(greaterTerm);
    raftModeManager.transitionToFollowerState();
  }

  /** Halts the candidate and election timer preventing anymore elections from occurring. */
  private void haltCandidate() {
    shouldContinueElections = false;
    raftElectionTimer.cancel();
  }

  private boolean receivedMajorityVotes(RequestVotesResults requestVotesResults) {
    int totalServers = raftClusterConfiguration.clusterServers().size();
    int votesReceived = 1 + requestVotesResults.numberVotesReceived();
    double requiredForMajority = totalServers / 2.0;
    return votesReceived > requiredForMajority;
  }
}
