package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.*;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;

/**
 * Handles the server's {@link RaftModeProcessor}s, transitioning between them, and relaying RPC
 * requests and election timeouts to the current one.
 */
@Singleton
final class RaftModeManager extends AbstractService
    implements RaftRpcHandler, RaftElectionTimeoutHandler, RaftCommandSubmitter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftConfiguration raftConfiguration;
  private final ListeningExecutorService executorService;
  private final RaftModeProcessor.Factory raftModeProcessorFactory;
  private final RaftVolatileState raftVolatileState;
  private final ReentrantLock transitionLock = new ReentrantLock();

  private volatile RaftModeProcessor raftModeProcessor;
  private volatile ListenableFuture<Void> runningProcessorFuture = Futures.immediateVoidFuture();

  @Inject
  RaftModeManager(
      RaftConfiguration raftConfiguration,
      @RaftModeManagerListeningExecutorService ListeningExecutorService executorService,
      RaftModeProcessor.Factory raftModeProcessorFactory,
      RaftVolatileState raftVolatileState) {
    this.raftConfiguration = raftConfiguration;
    this.executorService = executorService;
    this.raftModeProcessorFactory = raftModeProcessorFactory;
    this.raftVolatileState = raftVolatileState;
  }

  @Override
  protected void doStart() {
    // TODO: handle starting from persisted state
    transitionToFollowerState();
    notifyStarted();
  }

  @Override
  protected void doStop() {
    shutdown();
    notifyStopped();
  }

  private void doStopWithFailure(Throwable t) {
    shutdown();
    notifyFailed(t);
  }

  private void shutdown() {
    MoreExecutors.shutdownAndAwaitTermination(executorService, Duration.ofSeconds(5));
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
   * <p>This will cancel the currently running {@link RaftModeProcessor}.
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
   * <p>This will cancel the currently running {@link RaftModeProcessor}.
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
      raftModeProcessor = newRaftModeProcessor;
      runningProcessorFuture = Futures.submit(raftModeProcessor, executorService);
      Futures.addCallback(
          runningProcessorFuture,
          new ProcessorFutureCallback(getCurrentRaftMode()),
          executorService);
      if (isCurrentLeader()) {
        raftVolatileState.setLeaderId(raftConfiguration.thisRaftServerId());
      }
    } finally {
      logger.atInfo().log("Completed transition to [%s].", getCurrentRaftMode());
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
    public void onFailure(@Nonnull Throwable t) {
      if (t instanceof CancellationException) {
        // Tasks are manually cancelled
        return;
      }
      logger.atSevere().log("[%s] processor failed.", processorType);
      doStopWithFailure(t);
    }
  }
}
