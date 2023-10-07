package dev.sbutler.bitflask.storage.raft;

import static dev.sbutler.bitflask.storage.raft.RaftTimerUtils.*;

import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.time.Instant;
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

  private final RaftConfiguration raftConfiguration;

  private volatile Instant electionTimeout = Instant.MAX;

  @Inject
  RaftFollowerProcessor(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftConfiguration raftConfiguration) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.raftConfiguration = raftConfiguration;
  }

  @Override
  public RaftMode getRaftMode() {
    return RAFT_MODE;
  }

  @Override
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {
    int timeoutDelay = updateElectionTimeout();
    logger.atInfo().log(
        "Restarted election timer with delay of [%dms] after receiving RequestVote RPC.",
        timeoutDelay);
  }

  @Override
  protected void afterProcessRequestVoteRequest(RequestVoteRequest request, boolean voteGranted) {
    if (voteGranted) {
      int timeoutDelay = updateElectionTimeout();
      logger.atInfo().log(
          "Restarted election timer with delay of [%dms] after granting vote to [%s] for term [%d]",
          timeoutDelay, request.getCandidateId(), request.getTerm());
    }
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    int timeoutDelay = updateElectionTimeout();
    logger.atFine().atMostEvery(10, TimeUnit.SECONDS).log(
        "Restarted election timer with delay of [%dms] after receiving AppendEntries request",
        timeoutDelay);
  }

  @Override
  protected void afterProcessAppendEntriesRequest(AppendEntriesRequest request) {
    int timeoutDelay = updateElectionTimeout();
    logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log(
        "Restarted election timer with delay of [%dms] after processing AppendEntries request.",
        timeoutDelay);
  }

  @Override
  public void run() {
    int initialTimeoutDelay = updateElectionTimeout();
    logger.atInfo().log(
        "Started election timer with delay of [%dms] at start up.", initialTimeoutDelay);
    waitWithDynamicExpiration(() -> electionTimeout, () -> !shouldContinueExecuting());
    logger.atInfo().log("Election timer expired. Transition to Candidate.");
    raftModeManager.get().transitionToCandidateState();
  }

  private int updateElectionTimeout() {
    int delayMillis = getRandomDelayMillis(raftConfiguration.raftTimerInterval());
    electionTimeout = getExpirationFromNow(delayMillis);
    return delayMillis;
  }

  @Override
  protected FluentLogger getLogger() {
    return logger;
  }
}
