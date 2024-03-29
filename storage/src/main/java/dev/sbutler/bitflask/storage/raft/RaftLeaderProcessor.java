package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static dev.sbutler.bitflask.storage.raft.RaftTimerUtils.getExpirationFromNow;
import static dev.sbutler.bitflask.storage.raft.RaftTimerUtils.waitUntilExpiration;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.rpc.Code;
import com.google.rpc.Status;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.commands.StorageCommandExecutor;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftLeaderException;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Handles the {@link RaftMode#LEADER} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the Leader
 * mode.
 */
public final class RaftLeaderProcessor extends RaftModeProcessorBase
    implements RaftCommandSubmitter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final RaftMode RAFT_MODE = RaftMode.LEADER;
  private static final double REQUEST_BROADCAST_DELAY_RATIO = 0.75;

  private final ListeningExecutorService executorService;
  private final RaftConfiguration raftConfiguration;
  private final RaftLeaderState raftLeaderState;
  private final RaftLeaderRpcClient rpcClient;
  private final RaftSubmissionManager raftSubmissionManager;
  private final RaftEntryConverter raftEntryConverter;
  private final StorageCommandExecutor storageCommandExecutor;

  private final long requestBroadcastDelayMillis;

  @Inject
  RaftLeaderProcessor(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftLog raftLog,
      ListeningExecutorService executorService,
      RaftLeaderRpcClient.Factory rpcClientFactory,
      RaftConfiguration raftConfiguration,
      RaftLeaderState raftLeaderState,
      RaftSubmissionManager raftSubmissionManager,
      RaftEntryConverter raftEntryConverter,
      StorageCommandExecutor storageCommandExecutor) {
    super(raftModeManager, raftPersistentState, raftVolatileState, raftLog);
    this.executorService = executorService;
    this.raftConfiguration = raftConfiguration;
    this.raftLeaderState = raftLeaderState;
    this.rpcClient = rpcClientFactory.create(raftLeaderState);
    this.raftSubmissionManager = raftSubmissionManager;
    this.raftEntryConverter = raftEntryConverter;
    this.storageCommandExecutor = storageCommandExecutor;

    requestBroadcastDelayMillis =
        Math.round(
            raftConfiguration.raftTimerInterval().minimumMilliSeconds()
                * REQUEST_BROADCAST_DELAY_RATIO);
  }

  @Override
  public RaftMode getRaftMode() {
    return RAFT_MODE;
  }

  @Override
  protected FluentLogger getLogger() {
    return logger;
  }

  @Override
  protected void beforeUpdateTermAndTransitionToFollower(int rpcTerm) {
    logger.atWarning().log("Larger term [%d] found transitioning to follower.", rpcTerm);
    terminateExecution();
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    // A larger term was not found
    if (shouldContinueExecuting()) {
      // TODO: improve status code
      throw StatusProto.toStatusRuntimeException(
          Status.newBuilder()
              .setCode(Code.FAILED_PRECONDITION_VALUE)
              .setMessage(
                  String.format(
                      "This server is currently the leader. Request term [%d], local term [%d].",
                      request.getTerm(), raftPersistentState.getCurrentTerm()))
              .build());
    }
  }

  @Override
  public StorageSubmitResults submitCommand(StorageCommandDto storageCommandDto) {
    if (!storageCommandDto.isPersistable()) {
      ListenableFuture<StorageCommandResults> results =
          storageCommandExecutor.submitDto(storageCommandDto);
      return new StorageSubmitResults.Success(results);
    } else {
      Entry newEntry = raftEntryConverter.convert(storageCommandDto);
      int newEntryIndex = raftLog.appendEntry(newEntry);
      ListenableFuture<StorageCommandResults> results =
          raftSubmissionManager.addNewSubmission(newEntryIndex);
      return new StorageSubmitResults.Success(results);
    }
  }

  @Override
  public void run() {
    broadcastHeartbeat();
    while (shouldContinueExecuting() && !Thread.currentThread().isInterrupted()) {
      Instant waitExpiration = getExpirationFromNow(requestBroadcastDelayMillis);
      broadcastAppendEntriesOrHeartbeat();
      if (shouldContinueExecuting()) {
        checkAndUpdateCommitIndex();
      }
      // Prevent flooding network
      waitUntilExpiration(waitExpiration, () -> !shouldContinueExecuting());
    }
    cleanupBeforeTerminating();
  }

  /**
   * Updates the current commit index to the latest log entry that has been replicated to a majority
   * of servers.
   */
  private void checkAndUpdateCommitIndex() {
    int currentCommitIndex = raftVolatileState.getHighestCommittedEntryIndex();
    int currentTerm = raftPersistentState.getCurrentTerm();
    for (int possibleCommitIndex = raftLog.getLastLogEntryDetails().index();
        possibleCommitIndex > currentCommitIndex
            && raftLog.getEntryAtIndex(possibleCommitIndex).getTerm() == currentTerm;
        possibleCommitIndex--) {
      if (entryIndexHasMajorityMatch(possibleCommitIndex)) {
        raftVolatileState.increaseHighestCommittedEntryIndexTo(possibleCommitIndex);
        break;
      }
    }
  }

  /** Use to determine if an entry has been replicated to a majority of servers. */
  private boolean entryIndexHasMajorityMatch(int entryIndex) {
    double halfOfServers = raftConfiguration.clusterServers().size() / 2.0;
    long numServersWithEntryWithinMatch =
        1 // include this server
            + raftConfiguration.getOtherServersInCluster().keySet().stream()
                .map(raftLeaderState::getFollowerMatchIndex)
                .filter(matchIndex -> matchIndex >= entryIndex)
                .count();
    return numServersWithEntryWithinMatch > halfOfServers;
  }

  /** Holds the state of a single {@link AppendEntriesRequest} to a Raft server. */
  record AppendEntriesSubmission(
      RaftServerId serverId,
      int followerNextIndex,
      int lastEntryIndex,
      AppendEntriesRequest request,
      ListenableFuture<AppendEntriesResponse> responseFuture) {}

  /** Sends an {@link AppendEntriesRequest} with no {@link Entry}s to all followers. */
  private void broadcastHeartbeat() {
    Optional.of(rpcClient.broadcastHeartbeat())
        .flatMap(this::waitForAllResponsesToComplete)
        .ifPresent(this::handleCompletedResponses);
  }

  /** Appends {@link Entry}s to any follower who is behind the log; otherwise, a heartbeat. */
  private void broadcastAppendEntriesOrHeartbeat() {
    Optional.of(rpcClient.broadcastAppendEntriesOrHeartbeat())
        .flatMap(this::waitForAllResponsesToComplete)
        .ifPresent(this::handleCompletedResponses);
  }

  private Optional<ImmutableList<AppendEntriesSubmission>> waitForAllResponsesToComplete(
      ImmutableList<AppendEntriesSubmission> submissions) {
    try {
      // Block until all requests have completed or timed out
      Futures.whenAllComplete(
              submissions.stream()
                  .map(AppendEntriesSubmission::responseFuture)
                  .collect(toImmutableList()))
          .call(() -> null, executorService)
          .get();
      return Optional.of(submissions);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log(
          "Unexpected failure while waiting for response futures to complete. Exiting.");
      terminateExecution();
    }
    return Optional.empty();
  }

  private void handleCompletedResponses(ImmutableList<AppendEntriesSubmission> submissions) {
    int largestTermSeen = raftPersistentState.getCurrentTerm();
    for (var submission : submissions) {
      Optional<AppendEntriesResponse> response = getAppendEntriesResponse(submission);
      if (response.isEmpty()) {
        continue;
      }
      largestTermSeen = Math.max(largestTermSeen, response.get().getTerm());

      if (response.get().getSuccess()) {
        raftLeaderState.increaseFollowerNextIndex(submission);
        raftLeaderState.increaseFollowerMatchIndex(submission);
      } else {
        raftLeaderState.decreaseFollowerNextIndex(submission);
      }
      logAppendEntriesSubmissionResults(submission, response.get().getSuccess());
    }
    if (largestTermSeen > raftPersistentState.getCurrentTerm()) {
      updateTermAndTransitionToFollowerWithUnknownLeader(largestTermSeen);
    }
  }

  private Optional<AppendEntriesResponse> getAppendEntriesResponse(
      AppendEntriesSubmission submission) {
    try {
      return Optional.of(submission.responseFuture().get());
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).atMostEvery(10, TimeUnit.SECONDS).log(
          "Interrupted while sending request to follower [%s].", submission.serverId().id());
    } catch (ExecutionException e) {
      if (e.getCause() instanceof StatusRuntimeException statusException
          && io.grpc.Status.UNAVAILABLE.getCode().equals(statusException.getStatus().getCode())) {
        logger.atWarning().atMostEvery(5, TimeUnit.SECONDS).log(
            "Server [%s] unavailable for AppendEntries RPC.", submission.serverId().id());
      } else {
        logger.atSevere().withCause(e).atMostEvery(10, TimeUnit.SECONDS).log(
            "AppendEntries request to Follower [%s] with lastEntryIndex [%d] and followerNextIndex [%d] failed.",
            submission.serverId().id(),
            submission.lastEntryIndex(),
            submission.followerNextIndex());
      }
    }
    return Optional.empty();
  }

  private void logAppendEntriesSubmissionResults(
      AppendEntriesSubmission submission, boolean success) {
    String extraDetails =
        String.format(
            "[term=%d, lastEntryIndex=%d, followerNextIndex=%d].",
            submission.request().getTerm(),
            submission.lastEntryIndex(),
            submission.followerNextIndex());

    int entriesCount = submission.request().getEntriesCount();
    if (success) {
      if (entriesCount > 0) {
        logger.atInfo().log(
            "Follower [%s] accepted [%d] entries. %s",
            submission.serverId().id(), entriesCount, extraDetails);
      } else {
        logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log(
            "Follower [%s] accepted heartbeat. %s", submission.serverId().id(), extraDetails);
      }
    } else {
      if (entriesCount > 0) {
        logger.atWarning().log(
            "Follower [%s] did not accept [%d] entries. %s",
            submission.serverId().id(), entriesCount, extraDetails);
      } else {
        logger.atWarning().log(
            "Follower [%s] did not accept heartbeat. %s", submission.serverId().id(), extraDetails);
      }
    }
  }

  /**
   * Cleans up any unresolved client or {@link AppendEntriesResponse} futures before terminating.
   */
  private void cleanupBeforeTerminating() {
    raftSubmissionManager.completeAllSubmissionsWithFailure(
        new RaftLeaderException("Another leader was discovered"));
  }
}
