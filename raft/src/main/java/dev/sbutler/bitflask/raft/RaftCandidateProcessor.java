package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.RaftClusterCandidateRpcClient.RequestVotesResults;
import dev.sbutler.bitflask.raft.RaftLog.LastLogDetails;
import jakarta.inject.Inject;

final class RaftCandidateProcessor implements RaftModeProcessor {

  private final RaftClusterConfiguration raftClusterConfiguration;
  private final RaftModeManager raftModeManager;
  private final RaftPersistentState raftPersistentState;
  private final RaftElectionTimer raftElectionTimer;
  private final RaftClusterRpcChannelManager raftClusterRpcChannelManager;
  private final RaftLog raftLog;

  private volatile boolean continueElections = true;
  private volatile boolean cancelCurrentElection = false;

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
    cancelCurrentElection = true;
  }

  @Override
  public void run() {
    while (continueElections) {
      boolean wonElection = startNewElection();
      resetElectionTimerState();
      if (wonElection) {
        raftModeManager.transitionToLeaderState();
        return;
      }
    }
    raftModeManager.transitionToFollowerState();
  }

  private void resetElectionTimerState() {
    raftElectionTimer.cancel();
    cancelCurrentElection = false;
  }

  private boolean startNewElection() {
    raftPersistentState.incrementTermAndVoteForSelf();
    raftElectionTimer.restart();
    RaftClusterCandidateRpcClient candidateRpcClient =
        raftClusterRpcChannelManager.createRaftClusterCandidateRpcClient();
    try {
      return sendRequestVotesAndBlock(candidateRpcClient);
    } finally {
      candidateRpcClient.cancelRequests();
    }
  }

  /**
   * Requests a vote from all other Raft servers in the cluster and waits.
   *
   * <p>This method blocks until the election has been won, the election timer expires, or is
   * canceled by another thread.
   */
  private boolean sendRequestVotesAndBlock(RaftClusterCandidateRpcClient candidateRpcClient) {
    LastLogDetails lastLogDetails = raftLog.getLastLogDetails();
    RequestVoteRequest request =
        RequestVoteRequest.newBuilder()
            .setCandidateId(raftClusterConfiguration.thisRaftServerId().id())
            .setTerm(raftPersistentState.getCurrentTerm())
            .setLastLogIndex(lastLogDetails.index())
            .setLastLogTerm(lastLogDetails.term())
            .build();
    candidateRpcClient.requestVotes(request);

    RequestVotesResults requestVotesResults;
    do {
      requestVotesResults = candidateRpcClient.getCurrentRequestVotesResults();
      if (receivedMajorityVotes(requestVotesResults)) {
        return true;
      }
    } while (continueElections && !cancelCurrentElection);
    return false;
  }

  private boolean receivedMajorityVotes(RequestVotesResults requestVotesResults) {
    int totalServers = raftClusterConfiguration.clusterServers().size();
    int votesReceived = 1 + requestVotesResults.numberVotesReceived();
    double requiredForMajority = totalServers / 2.0;
    return votesReceived > requiredForMajority;
  }
}
