package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.*;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
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
final class RaftModeManager extends AbstractIdleService
    implements RaftRpcHandler, RaftElectionTimeoutHandler, RaftCommandSubmitter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftConfiguration raftConfiguration;
  private final RaftModeProcessor.Factory raftModeProcessorFactory;
  private final RaftElectionTimer raftElectionTimer;
  private final RaftVolatileState raftVolatileState;

  private final ListeningExecutorService executorService =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
  private final ReentrantLock transitionLock = new ReentrantLock();

  private volatile RaftModeProcessor raftModeProcessor;
  private volatile ListenableFuture<Void> runningProcessorFuture = Futures.immediateVoidFuture();

  @Inject
  RaftModeManager(
      RaftConfiguration raftConfiguration,
      RaftModeProcessor.Factory raftModeProcessorFactory,
      RaftElectionTimer raftElectionTimer,
      RaftVolatileState raftVolatileState) {
    this.raftConfiguration = raftConfiguration;
    this.raftModeProcessorFactory = raftModeProcessorFactory;
    this.raftElectionTimer = raftElectionTimer;
    this.raftVolatileState = raftVolatileState;
  }

  @Override
  protected void startUp() {
    // TODO: handle starting from persisted state
    transitionToFollowerState();
    //    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftFollowerProcessor());
  }

  @Override
  protected void shutDown() {
    runningProcessorFuture.cancel(false);
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

  public StorageSubmitResults submitCommand(StorageCommandDto storageCommandDto) {
    transitionLock.lock();
    try {
      if (isCurrentLeader()) {
        return ((RaftLeaderProcessor) raftModeProcessor).submitCommand(storageCommandDto);
      } else {
        return getCurrentLeaderServerInfo()
            .<StorageSubmitResults>map(StorageSubmitResults.NotCurrentLeader::new)
            .orElseGet(StorageSubmitResults.NoKnownLeader::new);
      }
    } finally {
      transitionLock.unlock();
    }
  }

  private RaftMode getCurrentRaftMode() {
    return raftModeProcessor.getRaftMode();
  }

  private boolean isCurrentLeader() {
    transitionLock.lock();
    try {
      return RaftMode.LEADER.equals(getCurrentRaftMode());
    } finally {
      transitionLock.unlock();
    }
  }

  private Optional<ServerConfig.ServerInfo> getCurrentLeaderServerInfo() {
    return raftVolatileState
        .getLeaderServerId()
        .map(leaderServiceId -> raftConfiguration.clusterServers().get(leaderServiceId));
  }

  /**
   * Transitions the server to use the {@link RaftFollowerProcessor}.
   *
   * <p>This will cancel the currently running {@link RaftModeProcessor} and updates the election
   * timer to call the new RaftFollowerProcessor when a timeout occurs.
   */
  void transitionToFollowerState() {
    Preconditions.checkState(
        raftModeProcessor == null || !RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE or LEADER state to transition to the FOLLOWER state.");

    logger.atInfo().log("Transitioning to Follower state.");
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
        raftModeProcessor == null || RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    logger.atInfo().log("Transitioning to Candidate state.");
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
        raftModeProcessor == null || RaftMode.CANDIDATE.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");

    logger.atInfo().log("Transitioning to Leader state.");
    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftLeaderProcessor());
  }

  private void transitionToNewRaftModeProcessor(RaftModeProcessor newRaftModeProcessor) {
    transitionLock.lock();
    try {
      runningProcessorFuture.cancel(false);
      raftElectionTimer.cancel();
      raftModeProcessor = newRaftModeProcessor;
      if (isCurrentLeader()) {
        raftVolatileState.setLeaderId(raftConfiguration.thisRaftServerId());
      }
      runningProcessorFuture = Futures.submit(raftModeProcessor, executorService);
      Futures.addCallback(
          runningProcessorFuture,
          new ProcessorFutureCallback(getCurrentRaftMode()),
          executorService);
    } finally {
      transitionLock.unlock();
    }
  }

  /**
   * Callback for handling the currently executing {@link RaftModeProcessor}'s execution results.
   */
  private final class ProcessorFutureCallback implements FutureCallback<Void> {
    private final RaftMode processorType;

    private ProcessorFutureCallback(RaftMode processorType) {
      this.processorType = processorType;
    }

    @Override
    public void onSuccess(Void result) {
      logger.atInfo().log("[%s] processor completed successfully.", processorType);
    }

    @Override
    public void onFailure(Throwable t) {
      logger.atSevere().withCause(t).log("[%s] processor failed. Shutting down.", processorType);
      shutDown();
    }
  }
}
