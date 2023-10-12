package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.*;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftModeException;
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
    implements RaftRpcHandler, RaftCommandSubmitter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftConfiguration raftConfiguration;
  private final ListeningExecutorService executorService;
  private final RaftModeProcessor.Factory raftModeProcessorFactory;
  private final RaftPersistentState raftPersistentState;
  private final RaftVolatileState raftVolatileState;
  private final ReentrantLock transitionLock = new ReentrantLock();

  private volatile RaftModeProcessor raftModeProcessor;
  private volatile ListenableFuture<Void> runningProcessorFuture = Futures.immediateVoidFuture();

  @Inject
  RaftModeManager(
      RaftConfiguration raftConfiguration,
      @RaftModeManagerListeningExecutorService ListeningExecutorService executorService,
      RaftModeProcessor.Factory raftModeProcessorFactory,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState) {
    this.raftConfiguration = raftConfiguration;
    this.executorService = executorService;
    this.raftModeProcessorFactory = raftModeProcessorFactory;
    this.raftPersistentState = raftPersistentState;
    this.raftVolatileState = raftVolatileState;
  }

  @Override
  protected void doStart() {
    // TODO: handle starting from persisted state
    try {
      transitionToFollowerState(raftPersistentState.getCurrentTerm(), Optional.empty());
      notifyStarted();
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to start RaftModeManager.");
      notifyFailed(e);
    }
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
   * Transitions the server to use the {@link RaftFollowerProcessor}, if not already, and updates
   * the leader service id, or clears it.
   *
   * @param transitionTerm the term this transition is valid for
   * @param knownLeaderServerId the server id of the current cluster leader
   */
  void transitionToFollowerState(int transitionTerm, Optional<RaftServerId> knownLeaderServerId) {
    transitionLock.lock();
    try {
      if (isUpToDateTransitionTerm(transitionTerm)) {
        knownLeaderServerId.ifPresentOrElse(
            raftVolatileState::setLeaderServerId, raftVolatileState::clearLeaderServerId);
        if (raftModeProcessor == null || !RaftMode.FOLLOWER.equals(getCurrentRaftMode())) {
          logger.atInfo().log("Transitioning to FOLLOWER state.");
          transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftFollowerProcessor());
        }
      } else {
        logger.atWarning().log(
            "Not transition to FOLLOWER state with an outdated term [%d], current term [%d], current Raft mode [%s].",
            transitionTerm, raftPersistentState.getCurrentTerm(), getCurrentRaftMode());
      }
    } finally {
      transitionLock.unlock();
    }
  }

  /**
   * Transitions the server to use the {@link RaftCandidateProcessor}.
   *
   * @param transitionTerm the term this transition is valid for
   */
  void transitionToCandidateState(int transitionTerm) {
    Preconditions.checkState(
        raftModeProcessor == null || RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    transitionLock.lock();
    try {
      if (isUpToDateTransitionTerm(transitionTerm)) {
        logger.atInfo().log("Transitioning to CANDIDATE state.");
        raftVolatileState.clearLeaderServerId();
        transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftCandidateProcessor());
      } else {
        logger.atWarning().log(
            "Not transition to CANDIDATE state with an outdated term [%d], current term [%d], current Raft mode [%s].",
            transitionTerm, raftPersistentState.getCurrentTerm(), getCurrentRaftMode());
      }
    } finally {
      transitionLock.unlock();
    }
  }

  /**
   * Transitions the server to use the {@link RaftLeaderProcessor}.
   *
   * @param transitionTerm the term this transition is valid for
   */
  void transitionToLeaderState(int transitionTerm) {
    Preconditions.checkState(
        raftModeProcessor == null || RaftMode.CANDIDATE.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");

    transitionLock.lock();
    try {
      if (isUpToDateTransitionTerm(transitionTerm)) {
        logger.atInfo().log("Transitioning to LEADER state.");
        raftVolatileState.setLeaderServerId(raftConfiguration.thisRaftServerId());
        transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftLeaderProcessor());
      } else {
        logger.atWarning().log(
            "Not transition to LEADER state with an outdated term [%d], current term [%d], current Raft mode [%s].",
            transitionTerm, raftPersistentState.getCurrentTerm(), getCurrentRaftMode());
      }
    } finally {
      transitionLock.unlock();
    }
  }

  /**
   * Returns true of the provided {@code transitionTerm} matches the current term.
   *
   * <p>A {@link RaftModeException} will be thrown if a greater term was provided than the current
   * term.
   */
  private boolean isUpToDateTransitionTerm(int transitionTerm) {
    int currentTerm = raftPersistentState.getCurrentTerm();
    if (transitionTerm == currentTerm) {
      return true;
    } else if (transitionTerm < currentTerm) {
      return false;
    } else {
      throw new RaftModeException(
          String.format(
              "Transition term [%d] is greater than the current term [%d]. Current Raft mode [%s].",
              transitionTerm, currentTerm, getCurrentRaftMode()));
    }
  }

  private void transitionToNewRaftModeProcessor(RaftModeProcessor newRaftModeProcessor) {
    transitionLock.lock();
    try {
      runningProcessorFuture.cancel(true);
      raftModeProcessor = newRaftModeProcessor;
      runningProcessorFuture = Futures.submit(raftModeProcessor, executorService);
      Futures.addCallback(
          runningProcessorFuture,
          new ProcessorFutureCallback(getCurrentRaftMode()),
          executorService);
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
      logger.atSevere().withCause(t).log("[%s] processor failed.", processorType);
      doStopWithFailure(t);
    }
  }
}
