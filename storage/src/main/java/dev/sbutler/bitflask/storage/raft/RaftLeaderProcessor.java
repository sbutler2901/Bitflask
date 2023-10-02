package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.rpc.Code;
import com.google.rpc.Status;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.commands.StorageCommandExecutor;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftLeaderException;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftUnknownLeaderException;
import io.grpc.protobuf.StatusProto;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
  private static final double REQUEST_TIMEOUT_RATIO = 0.75;

  private final ScheduledExecutorService executorService;
  private final RaftConfiguration raftConfiguration;
  private final RaftLog raftLog;
  private final RaftClusterRpcChannelManager raftClusterRpcChannelManager;
  private final RaftSubmissionManager raftSubmissionManager;
  private final RaftEntryConverter raftEntryConverter;
  private final StorageCommandExecutor storageCommandExecutor;

  private final ConcurrentMap<RaftServerId, AtomicInteger> followersNextIndex =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<RaftServerId, AtomicInteger> followersMatchIndex =
      new ConcurrentHashMap<>();

  private final Duration requestTimeoutDuration;

  private volatile boolean shouldContinueExecuting = true;

  @Inject
  RaftLeaderProcessor(
      Provider<RaftModeManager> raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      ScheduledExecutorService executorService,
      RaftConfiguration raftConfiguration,
      RaftLog raftLog,
      RaftClusterRpcChannelManager raftClusterRpcChannelManager,
      RaftSubmissionManager raftSubmissionManager,
      RaftEntryConverter raftEntryConverter,
      StorageCommandExecutor storageCommandExecutor) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.executorService = executorService;
    this.raftConfiguration = raftConfiguration;
    this.raftLog = raftLog;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
    this.raftSubmissionManager = raftSubmissionManager;
    this.raftEntryConverter = raftEntryConverter;
    this.storageCommandExecutor = storageCommandExecutor;

    requestTimeoutDuration =
        Duration.ofMillis(
            Math.round(
                raftConfiguration.raftTimerInterval().minimumMilliSeconds()
                    * REQUEST_TIMEOUT_RATIO));

    int nextIndex = raftLog.getLastLogEntryDetails().index() + 1;
    for (var followerServerId : raftConfiguration.getOtherServersInCluster().keySet()) {
      followersNextIndex.put(followerServerId, new AtomicInteger(nextIndex));
      followersMatchIndex.put(followerServerId, new AtomicInteger(0));
    }
  }

  @Override
  public RaftMode getRaftMode() {
    return RAFT_MODE;
  }

  private void handleUnexpectedRequest(String additionalMessage) {
    throw StatusProto.toStatusRuntimeException(
        Status.newBuilder()
            .setCode(Code.FAILED_PRECONDITION_VALUE)
            .setMessage(
                "This server is currently the leader and requests should not be sent to it. "
                    + additionalMessage)
            .build());
  }

  @Override
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {
    handleUnexpectedRequest(
        String.format(
            "Request term [%d], local term [%d].",
            request.getTerm(), raftPersistentState.getCurrentTerm()));
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    handleUnexpectedRequest(
        String.format(
            "Request term [%d], local term [%d].",
            request.getTerm(), raftPersistentState.getCurrentTerm()));
  }

  @Override
  protected void beforeUpdateTermAndTransitionToFollower(int rpcTerm) {
    logger.atWarning().log("Larger term [%d] found transitioning to follower.", rpcTerm);
    shouldContinueExecuting = false;
  }

  @Override
  public void handleElectionTimeout() {
    throw new IllegalStateException(
        "Raft in LEADER mode should not have an election timer running");
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
    sendHeartbeatToAll();
    while (shouldContinueExecuting) {
      appendEntriesOrSendHeartbeatToEach();
      if (shouldContinueExecuting) {
        checkAndUpdateCommitIndex();
      }
      // TODO: prevent flooding network
    }
    cleanupBeforeTerminating();
  }

  /** Sends an {@link AppendEntriesRequest} with no {@link Entry}s to all followers. */
  private void sendHeartbeatToAll() {
    ImmutableList<AppendEntriesSubmission> submissions =
        raftConfiguration.getOtherServersInCluster().keySet().stream()
            .map(
                raftServerId -> {
                  int nextIndex = followersNextIndex.get(raftServerId).get();
                  return submitAppendEntriesToServer(raftServerId, nextIndex, nextIndex);
                })
            .collect(toImmutableList());
    waitForAllSubmissionsAndHandle(submissions);
  }

  /** Appends {@link Entry}s to any follower who is behind the log; otherwise, a heartbeat. */
  private void appendEntriesOrSendHeartbeatToEach() {
    int lastEntryIndex = raftLog.getLastLogEntryDetails().index();
    ImmutableList<AppendEntriesSubmission> submissions =
        raftConfiguration.getOtherServersInCluster().keySet().stream()
            .map(
                raftServerId ->
                    submitAppendEntriesToServer(
                        raftServerId,
                        followersNextIndex.get(raftServerId).get(),
                        1 + lastEntryIndex))
            .collect(toImmutableList());
    waitForAllSubmissionsAndHandle(submissions);
  }

  private record AppendEntriesSubmission(
      RaftServerId serverId,
      int followerNextIndex,
      int lastEntryIndex,
      AppendEntriesRequest request,
      ListenableFuture<AppendEntriesResponse> responseFuture) {}

  private AppendEntriesSubmission submitAppendEntriesToServer(
      RaftServerId serverId, int followerNextIndex, int lastEntryIndex) {
    ImmutableList<Entry> entries = raftLog.getEntriesFromIndex(followerNextIndex, lastEntryIndex);
    AppendEntriesRequest request =
        createBaseAppendEntriesRequest(followerNextIndex - 1).addAllEntries(entries).build();
    RaftClusterLeaderRpcClient leaderRpcClient =
        raftClusterRpcChannelManager.createRaftClusterLeaderRpcClient();
    ListenableFuture<AppendEntriesResponse> responseFuture =
        Futures.withTimeout(
            leaderRpcClient.appendEntries(serverId, request),
            requestTimeoutDuration,
            executorService);
    return new AppendEntriesSubmission(
        serverId, followerNextIndex, lastEntryIndex, request, responseFuture);
  }

  /**
   * Creates an {@link dev.sbutler.bitflask.storage.raft.AppendEntriesRequest.Builder} without the
   * {@link dev.sbutler.bitflask.storage.raft.Entry} list populated.
   */
  private AppendEntriesRequest.Builder createBaseAppendEntriesRequest(int prevLogIndex) {
    String leaderId =
        raftVolatileState
            .getLeaderServerId()
            .map(RaftServerId::id)
            .orElseThrow(
                () ->
                    new RaftUnknownLeaderException(
                        "LeaderServerId was not set for use by the RaftLeaderProcessor"));

    return AppendEntriesRequest.newBuilder()
        .setTerm(raftPersistentState.getCurrentTerm())
        .setLeaderId(leaderId)
        .setPrevLogTerm(raftLog.getEntryAtIndex(prevLogIndex).getTerm())
        .setPrevLogIndex(prevLogIndex);
  }

  private void waitForAllSubmissionsAndHandle(ImmutableList<AppendEntriesSubmission> submissions) {
    try {
      // Block until all requests have completed or timed out
      Futures.whenAllComplete(
              submissions.stream()
                  .map(AppendEntriesSubmission::responseFuture)
                  .collect(toImmutableList()))
          .call(() -> null, executorService)
          .get();
    } catch (Exception e) {
      logger.atSevere().withCause(e).log(
          "Unexpected failure while waiting for response futures to complete");
    }
    int largestTermSeen = raftPersistentState.getCurrentTerm();
    for (var submission : submissions) {
      int term = handleAppendEntriesSubmission(submission);
      largestTermSeen = Math.max(largestTermSeen, term);
    }
    if (largestTermSeen > raftPersistentState.getCurrentTerm()) {
      updateTermAndTransitionToFollower(largestTermSeen);
    }
  }

  private int handleAppendEntriesSubmission(AppendEntriesSubmission submission) {
    AppendEntriesResponse response;
    try {
      response = submission.responseFuture().get();
    } catch (Exception e) {
      logger.atSevere().withCause(e).atMostEvery(10, TimeUnit.SECONDS).log(
          "AppendEntries request to Follower [%s] with lastEntryIndex [%d] and followerNextIndex [%d] failed.",
          submission.serverId().id(), submission.lastEntryIndex(), submission.followerNextIndex());
      return raftPersistentState.getCurrentTerm();
    }

    if (!response.getSuccess() && response.getTerm() > raftPersistentState.getCurrentTerm()) {
      return response.getTerm();
    } else if (!response.getSuccess()) {
      followersNextIndex
          .get(submission.serverId())
          .getAndUpdate(prev -> Math.min(prev, submission.followerNextIndex() - 1));
      logger.atInfo().atMostEvery(1, TimeUnit.SECONDS).log(
          "Follower [%s] did not accept AppendEntries request with term [%d], lastEntryIndex [%d], and followerNextIndex [%d].",
          submission.serverId().id(),
          submission.request().getTerm(),
          submission.lastEntryIndex(),
          submission.followerNextIndex());
    } else {
      followersNextIndex
          .get(submission.serverId())
          .getAndUpdate(prev -> Math.max(prev, submission.lastEntryIndex()));
      followersMatchIndex
          .get(submission.serverId())
          .getAndUpdate(prev -> Math.max(prev, submission.lastEntryIndex()));
      logger.atInfo().atMostEvery(5, TimeUnit.SECONDS).log(
          "Follower [%s] accepted lastEntryIndex [%d] with prevLogIndex [%d].",
          submission.serverId().id(), submission.lastEntryIndex(), submission.followerNextIndex());
    }
    return raftPersistentState.getCurrentTerm();
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
        raftVolatileState.setHighestCommittedEntryIndex(possibleCommitIndex);
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
                .map(followersMatchIndex::get)
                .map(AtomicInteger::get)
                .filter(matchIndex -> matchIndex >= entryIndex)
                .count();
    return numServersWithEntryWithinMatch > halfOfServers;
  }

  /**
   * Cleans up any unresolved client or {@link AppendEntriesResponse} futures before terminating.
   */
  private void cleanupBeforeTerminating() {
    raftSubmissionManager.completeAllSubmissionsWithFailure(
        new RaftLeaderException("Another leader was discovered"));
  }
}
