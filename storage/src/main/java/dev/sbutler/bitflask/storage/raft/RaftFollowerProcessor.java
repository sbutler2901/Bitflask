package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.concurrent.TimeUnit;

/**
 * Handles the {@link RaftMode#FOLLOWER} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the
 * follower mode.
 */
public final class RaftFollowerProcessor extends RaftModeProcessorBase {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final RaftMode RAFT_MODE = RaftMode.FOLLOWER;

  private final RaftElectionTimer raftElectionTimer;

  @Inject
  RaftFollowerProcessor(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftElectionTimer raftElectionTimer) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.raftElectionTimer = raftElectionTimer;
  }

  @Override
  public RaftMode getRaftMode() {
    return RAFT_MODE;
  }

  @Override
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {
    int timerDelay = raftElectionTimer.restart();
    logger.atInfo().log(
        "Restarted election timer with delay of [%dms] after receiving RequestVote RPC.",
        timerDelay);
  }

  @Override
  protected void afterProcessRequestVoteRequest(RequestVoteRequest request, boolean voteGranted) {
    if (voteGranted) {
      int timerDelay = raftElectionTimer.restart();
      logger.atInfo().log(
          "Restarted election timer with delay of [%dms] after granting vote to [%s] for term [%d]",
          timerDelay, request.getCandidateId(), request.getTerm());
    }
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    int timerDelay = raftElectionTimer.restart();
    logger.atFine().atMostEvery(10, TimeUnit.SECONDS).log(
        "Restarted election timer with delay of [%dms] after receiving AppendEntries request",
        timerDelay);
  }

  @Override
  protected void afterProcessAppendEntriesRequest(
      AppendEntriesRequest request, boolean appendSuccessful) {
    int timerDelay = raftElectionTimer.restart();
    logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log(
        "Restarted election timer with delay of [%dms] after AppendEntries [%s].",
        timerDelay, appendSuccessful ? "success" : "failed");
  }

  @Override
  public void handleElectionTimeout() {
    logger.atInfo().log("Handling election timeout.");
    raftModeManager.get().transitionToCandidateState();
  }

  @Override
  public void run() {
    int timerDelay = raftElectionTimer.restart();
    logger.atInfo().log("Restarted election timer with delay of [%dms] at start up.", timerDelay);
    // Nothing for follower to do besides process incoming RPCs
  }
}
