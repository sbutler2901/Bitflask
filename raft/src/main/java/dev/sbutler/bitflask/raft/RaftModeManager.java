package dev.sbutler.bitflask.raft;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles the server's {@link RaftModeProcessor}s, transitioning between them, and relaying RPC
 * requests and election timeouts to the current one.
 */
@Singleton
final class RaftModeManager
    implements RaftRpcHandler, RaftElectionTimeoutHandler, RaftCommandSubmitter {

  private final RaftConfigurations raftConfigurations;
  private final RaftModeProcessor.Factory raftModeProcessorFactory;
  private final RaftElectionTimer raftElectionTimer;
  private final RaftVolatileState raftVolatileState;

  private final ListeningExecutorService executorService =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
  private final ReentrantLock transitionLock = new ReentrantLock();

  private volatile RaftModeProcessor raftModeProcessor;
  private volatile ListenableFuture<?> runningProcessorFuture = Futures.immediateVoidFuture();

  @Inject
  RaftModeManager(
      RaftConfigurations raftConfigurations,
      RaftModeProcessor.Factory raftModeProcessorFactory,
      RaftElectionTimer raftElectionTimer,
      RaftVolatileState raftVolatileState) {
    this.raftConfigurations = raftConfigurations;
    this.raftModeProcessorFactory = raftModeProcessorFactory;
    this.raftElectionTimer = raftElectionTimer;
    this.raftVolatileState = raftVolatileState;
    // TODO: handle starting from persisted state
    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftFollowerProcessor());
  }

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    transitionLock.lock();
    try {
      return raftModeProcessor.processRequestVoteRequest(request);
    } finally {
      transitionLock.unlock();
    }
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    transitionLock.lock();
    try {
      return raftModeProcessor.processAppendEntriesRequest(request);
    } finally {
      transitionLock.unlock();
    }
  }

  /** Updates the Raft's server state as a result of an election timer timeout. */
  public void handleElectionTimeout() {
    transitionLock.lock();
    try {
      raftModeProcessor.handleElectionTimeout();
    } finally {
      transitionLock.unlock();
    }
  }

  public RaftSubmitResults submitCommand(RaftCommand raftCommand) {
    transitionLock.lock();
    try {
      if (isCurrentLeader()) {
        return ((RaftLeaderProcessor) raftModeProcessor).submitCommand(raftCommand);
      } else {
        return getCurrentLeaderServerInfo()
            .<RaftSubmitResults>map(RaftSubmitResults.NotCurrentLeader::new)
            .orElseGet(RaftSubmitResults.NoKnownLeader::new);
      }
    } finally {
      transitionLock.unlock();
    }
  }

  boolean isCurrentLeader() {
    transitionLock.lock();
    try {
      return RaftMode.LEADER.equals(getCurrentRaftMode());
    } finally {
      transitionLock.unlock();
    }
  }

  Optional<RaftServerInfo> getCurrentLeaderServerInfo() {
    return raftVolatileState
        .getLeaderServerId()
        .map(leaderServiceId -> raftConfigurations.clusterServers().get(leaderServiceId));
  }

  /**
   * Transitions the server to use the {@link RaftFollowerProcessor}.
   *
   * <p>This will cancel the currently running {@link RaftModeProcessor} and updates the election
   * timer to call the new RaftFollowerProcessor when a timeout occurs.
   */
  void transitionToFollowerState() {
    Preconditions.checkState(
        !RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE or LEADER state to transition to the FOLLOWER state.");

    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftFollowerProcessor());
  }

  /**
   * Transitions the server to use the {@link RaftCandidateProcessor}.
   *
   * <p>This will cancel the currently running {@link RaftModeProcessor} and {@link
   * RaftElectionTimer}.
   */
  void transitionToCandidateState() {
    Preconditions.checkState(
        RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftCandidateProcessor());
  }

  /**
   * Transitions the server to use the {@link RaftLeaderProcessor}.
   *
   * <p>This will cancel the currently running {@link RaftModeProcessor} and {@link
   * RaftElectionTimer}.
   */
  void transitionToLeaderState() {
    Preconditions.checkState(
        RaftMode.CANDIDATE.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");

    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftLeaderProcessor());
  }

  private void transitionToNewRaftModeProcessor(RaftModeProcessor newRaftModeProcessor) {
    transitionLock.lock();
    try {
      runningProcessorFuture.cancel(false);
      raftElectionTimer.cancel();
      raftModeProcessor = newRaftModeProcessor;
      if (isCurrentLeader()) {
        raftVolatileState.setLeaderId(raftConfigurations.thisRaftServerId());
      }
      runningProcessorFuture = executorService.submit(raftModeProcessor);
    } finally {
      transitionLock.unlock();
    }
  }

  /** Returns the current {@link RaftMode} of the server. */
  private RaftMode getCurrentRaftMode() {
    return switch (raftModeProcessor) {
      case RaftFollowerProcessor _unused -> RaftMode.FOLLOWER;
      case RaftCandidateProcessor _unused -> RaftMode.CANDIDATE;
      case RaftLeaderProcessor _unused -> RaftMode.LEADER;
    };
  }

  /** Represents the modes a Raft server can be in. */
  enum RaftMode {
    /** The server is only listening from incoming RPCs. */
    FOLLOWER,
    /** The server is attempting to become the leader of the cluster. */
    CANDIDATE,
    /** The server is receiving client requests and replicating across the cluster. */
    LEADER
  }
}
