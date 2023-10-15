package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static dev.sbutler.bitflask.storage.raft.RaftTimerUtils.*;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.grpc.StatusRuntimeException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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

  @Inject
  RaftCandidateProcessor(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftLog raftLog,
      ListeningExecutorService executorService,
      RaftConfiguration raftConfiguration,
      RaftCandidateRpcClient.Factory rpcClientFactory) {
    super(raftModeManager, raftPersistentState, raftVolatileState, raftLog);
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
    logger.atWarning().log("Larger term [%d] found. Transitioning to FOLLOWER state.", rpcTerm);
    terminateExecution();
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    // Concede to new leader
    if (request.getTerm() >= raftPersistentState.getCurrentTerm()) {
      terminateExecution();
      updateTermAndTransitionToFollower(
          request.getTerm(), Optional.of(new RaftServerId(request.getLeaderId())));
    }
  }

  /**
   * The main loop of this class that executes until the RequestVotes RPCs sent cause termination,
   * or an RPC is received from another server that causes termination.
   */
  @Override
  public void run() {
    while (shouldContinueExecuting() && !Thread.currentThread().isInterrupted()) {
      raftPersistentState.incrementTermAndVoteForSelf();
      int electionTimeout = getRandomDelayMillis(raftConfiguration.raftTimerInterval());
      logger.atInfo().log(
          "Starting new election term [%d] with election timer delay [%dms].",
          raftPersistentState.getCurrentTerm(), electionTimeout);

      boolean receivedMajorityVotes = requestVotes(electionTimeout);
      if (receivedMajorityVotes) {
        terminateExecution();
        logger.atInfo().log("Received majority of votes. Transitioning to Leader");
        raftModeManager.get().transitionToLeaderState(raftPersistentState.getCurrentTerm());
      } else {
        waitUntilExpiration(
            getExpirationFromNow(electionTimeout), () -> !shouldContinueExecuting());
      }
    }
  }

  record RequestVotesSubmission(
      RaftServerId serverId,
      RequestVoteRequest request,
      ListenableFuture<RequestVoteResponse> responseFuture) {}

  /** Starts and handles a single election cycle returning true if the election was won. */
  private boolean requestVotes(int electionTimeout) {
    return Optional.of(rpcClient.broadcastRequestVotes(electionTimeout))
        .flatMap(this::waitForAllResponsesToComplete)
        .flatMap(this::handleCompletedResponses)
        .map(this::didReceivedMajorityVotes)
        .orElse(false);
  }

  private Optional<ImmutableList<RequestVotesSubmission>> waitForAllResponsesToComplete(
      ImmutableList<RequestVotesSubmission> submissions) {
    try {
      // Block until all requests have completed or timed out
      Futures.whenAllComplete(
              submissions.stream()
                  .map(RequestVotesSubmission::responseFuture)
                  .collect(toImmutableList()))
          .call(() -> null, executorService)
          .get();
      return Optional.of(submissions);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log(
          "Unexpected failure while waiting for response futures to complete. Exiting");
      terminateExecution();
    }
    return Optional.empty();
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
      updateTermAndTransitionToFollowerWithUnknownLeader(largestTermSeen);
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

  private boolean didReceivedMajorityVotes(int votesReceived) {
    double halfRequiredVotes = raftConfiguration.getOtherServersInCluster().keySet().size() / 2.0;
    return (1 + votesReceived) > halfRequiredVotes;
  }

  @Override
  protected FluentLogger getLogger() {
    return logger;
  }
}
