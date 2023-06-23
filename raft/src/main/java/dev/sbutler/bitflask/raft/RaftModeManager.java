package dev.sbutler.bitflask.raft;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/** Handles Raft's server mode and transitioning between them. */
@Singleton
final class RaftModeManager implements HandlesElectionTimeout {

  private final RaftModeProcessorFactory raftModeProcessorFactory;

  private final ListeningExecutorService executorService =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
  private final ReentrantLock transitionLock = new ReentrantLock();

  private volatile RaftModeProcessor raftModeProcessor;
  private volatile ListenableFuture<?> runningProcessorFuture = Futures.immediateVoidFuture();

  @Inject
  RaftModeManager(RaftModeProcessorFactory raftModeProcessorFactory) {
    this.raftModeProcessorFactory = raftModeProcessorFactory;
    // TODO: handle starting from persisted state
    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftFollowerProcessor());
  }

  /** Returns the current {@link RaftMode} of the server. */
  RaftMode getCurrentRaftMode() {
    return switch (raftModeProcessor) {
      case RaftFollowerProcessor _unused -> RaftMode.FOLLOWER;
      case RaftCandidateProcessor _unused -> RaftMode.CANDIDATE;
      case RaftLeaderProcessor _unused -> RaftMode.LEADER;
    };
  }

  /**
   * Returns the current {@link RaftModeProcessor} based on the current {@link RaftMode} of the
   * server.
   */
  RaftModeProcessor getRaftStateProcessor() {
    return raftModeProcessor;
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

  /** Transitions the server to the {@link RaftMode#FOLLOWER} state. */
  void transitionToFollowerState() {
    Preconditions.checkState(
        !RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE or LEADER state to transition to the FOLLOWER state.");

    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftFollowerProcessor());
  }

  /** Transitions the server to the {@link RaftMode#CANDIDATE} state. */
  void transitionToCandidateState() {
    Preconditions.checkState(
        RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    transitionToNewRaftModeProcessor(raftModeProcessorFactory.createRaftCandidateProcessor());
  }

  /** Transitions the server to the {@link RaftMode#LEADER} state. */
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
      raftModeProcessor = newRaftModeProcessor;
      runningProcessorFuture = executorService.submit(raftModeProcessor);
    } finally {
      transitionLock.unlock();
    }
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
