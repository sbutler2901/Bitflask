package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.grpc.StatusRuntimeException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Handles the {@link RaftMode#CANDIDATE} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the
 * Candidate mode.
 */
public final class RaftCandidateProcessor extends RaftModeProcessorBase {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final RaftMode RAFT_MODE = RaftMode.CANDIDATE;

  private final ListeningExecutorService executorService;
  private final RaftConfiguration raftConfiguration;
  private final RaftCandidateRpcClient rpcClient;

  private volatile boolean shouldContinueElections = true;

  @Inject
  RaftCandidateProcessor(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      ListeningExecutorService executorService,
      RaftConfiguration raftConfiguration,
      RaftCandidateRpcClient.Factory rpcClientFactory) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.executorService = executorService;
    this.raftConfiguration = raftConfiguration;
    this.rpcClient = rpcClientFactory.create();
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
  }

  /**
   * The main loop of this class that executes until the RequestVotes RPCs sent cause termination,
   * or an RPC is received from another server that causes termination.
   */
  @Override
  public void run() {
    while (shouldContinueElections) {
      int electionTimeout = getElectionTimeout();
      Instant electionExpiration = Instant.now().plusMillis(electionTimeout);

      boolean receivedMajorityVotes = startNewElection(electionTimeout);
      if (receivedMajorityVotes) {
        shouldContinueElections = false;
        logger.atInfo().log("Received majority of votes. Transitioning to Leader");
        raftModeManager.get().transitionToLeaderState();
      } else {
        while (shouldContinueElections && !Instant.now().isAfter(electionExpiration)) {
          Thread.onSpinWait();
        }
      }
    }
  }

  private int getElectionTimeout() {
    RaftTimerInterval raftTimerInterval = raftConfiguration.raftTimerInterval();
    return ThreadLocalRandom.current()
        .nextInt(
            raftTimerInterval.minimumMilliSeconds(), 1 + raftTimerInterval.maximumMilliseconds());
  }

  record RequestVotesSubmission(
      RaftServerId serverId,
      RequestVoteRequest request,
      ListenableFuture<RequestVoteResponse> responseFuture) {}

  /** Starts and handles a single election cycle. */
  private boolean startNewElection(int electionTimeout) {
    raftPersistentState.incrementTermAndVoteForSelf();
    logger.atInfo().log(
        "Started new election term [%d] with election timer delay [%dms].",
        raftPersistentState.getCurrentTerm(), electionTimeout);
    ImmutableList<RequestVotesSubmission> submissions =
        rpcClient.broadcastRequestVotes(electionTimeout);
    waitForAllResponsesToComplete(submissions);
    return handleCompletedResponses(submissions).map(this::receivedMajorityVotes).orElse(false);
  }

  private void waitForAllResponsesToComplete(ImmutableList<RequestVotesSubmission> submissions) {
    try {
      // Block until all requests have completed or timed out
      Futures.whenAllComplete(
              submissions.stream()
                  .map(RequestVotesSubmission::responseFuture)
                  .collect(toImmutableList()))
          .call(() -> null, executorService)
          .get();
    } catch (Exception e) {
      logger.atSevere().withCause(e).log(
          "Unexpected failure while waiting for response futures to complete. Exiting");
    }
  }

  private Optional<Integer> handleCompletedResponses(
      ImmutableList<RequestVotesSubmission> submissions) {
    int largestTermSeen = raftPersistentState.getCurrentTerm();
    int votesReceived = 0;
    for (var submission : submissions) {
      Optional<RequestVoteResponse> response = getRequestVotesResponse(submission);
      if (response.isEmpty()) {
        continue;
      }
      largestTermSeen = Math.max(largestTermSeen, response.get().getTerm());
      if (response.get().getVoteGranted()) {
        votesReceived += 1;
        logger.atInfo().log(
            "Vote granted by [%s] for term [%d].",
            submission.serverId().id(), submission.request().getTerm());
      } else {
        logger.atInfo().log(
            "Voted denied by [%s] for term [%d].",
            submission.serverId().id(), submission.request().getTerm());
      }
    }

    if (largestTermSeen > raftPersistentState.getCurrentTerm()) {
      updateTermAndTransitionToFollower(largestTermSeen);
      return Optional.empty();
    }
    return Optional.of(votesReceived);
  }

  private Optional<RequestVoteResponse> getRequestVotesResponse(RequestVotesSubmission submission) {
    try {
      return Optional.of(submission.responseFuture().get());
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).atMostEvery(10, TimeUnit.SECONDS).log(
          "Interrupted while sending request to server [%s].", submission.serverId().id());
    } catch (ExecutionException e) {
      if (e.getCause() instanceof StatusRuntimeException statusException
          && io.grpc.Status.UNAVAILABLE.getCode().equals(statusException.getStatus().getCode())) {
        logger.atWarning().atMostEvery(5, TimeUnit.SECONDS).log(
            "Server [%s] unavailable for RequestVotes RPC.", submission.serverId().id());
      } else {
        logger.atSevere().withCause(e).atMostEvery(10, TimeUnit.SECONDS).log(
            "RequestVotes request to server [%s] with term [%d], lastLogIndex [%d], and lastLogTerm [%d] failed.",
            submission.serverId(),
            submission.request().getTerm(),
            submission.request().getLastLogIndex(),
            submission.request().getLastLogTerm());
      }
    }
    return Optional.empty();
  }

  private boolean receivedMajorityVotes(int votesReceived) {
    double halfRequiredVotes = raftConfiguration.getOtherServersInCluster().keySet().size() / 2.0;
    return (1 + votesReceived) > halfRequiredVotes;
  }

  @Override
  protected FluentLogger getLogger() {
    return logger;
  }
}
