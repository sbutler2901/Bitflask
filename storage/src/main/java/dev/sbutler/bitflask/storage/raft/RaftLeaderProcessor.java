package dev.sbutler.bitflask.storage.raft;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.rpc.Code;
import com.google.rpc.Status;
import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftLeaderException;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftUnknownLeaderException;
import io.grpc.protobuf.StatusProto;
import jakarta.inject.Inject;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the {@link RaftModeManager.RaftMode#LEADER} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the Leader
 * mode.
 */
final class RaftLeaderProcessor extends RaftModeProcessorBase implements RaftCommandSubmitter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final RaftLog raftLog;
  private final RaftClusterRpcChannelManager raftClusterRpcChannelManager;
  private final RaftCommandConverter raftCommandConverter;
  private final RaftConfiguration raftConfiguration;

  private final ConcurrentMap<RaftServerId, AtomicInteger> followersNextIndex =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<RaftServerId, AtomicInteger> followersMatchIndex =
      new ConcurrentHashMap<>();
  private final NavigableSet<WaitingSubmission> waitingSubmissions = new ConcurrentSkipListSet<>();
  private final Set<ListenableFuture<AppendEntriesResponse>> responseFutures =
      ConcurrentHashMap.newKeySet();

  private volatile boolean shouldContinueExecuting = true;

  @Inject
  RaftLeaderProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      ListeningExecutorService executorService,
      RaftLog raftLog,
      RaftClusterRpcChannelManager raftClusterRpcChannelManager,
      RaftCommandConverter raftCommandConverter,
      RaftConfiguration raftConfiguration) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.executorService = executorService;
    this.raftLog = raftLog;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
    this.raftCommandConverter = raftCommandConverter;
    this.raftConfiguration = raftConfiguration;

    int nextIndex = raftLog.getLastEntryIndex() + 1;
    for (var followerServerId : raftConfiguration.getOtherServersInCluster().keySet()) {
      followersNextIndex.put(followerServerId, new AtomicInteger(nextIndex));
      followersMatchIndex.put(followerServerId, new AtomicInteger(0));
    }
  }

  private void handleUnexpectedRequest() {
    throw StatusProto.toStatusRuntimeException(
        Status.newBuilder()
            .setCode(Code.FAILED_PRECONDITION_VALUE)
            .setMessage("This server is currently the leader and requests should not be sent to it")
            .build());
  }

  @Override
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {
    handleUnexpectedRequest();
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    handleUnexpectedRequest();
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
  public StorageSubmitResults submitCommand(StorageCommand storageCommand) {
    Entry newEntry = raftCommandConverter.convert(raftCommand);
    int newEntryIndex = raftLog.appendEntry(newEntry);
    SettableFuture<String> clientSubmitFuture = SettableFuture.create();
    waitingSubmissions.add(new WaitingSubmission(newEntryIndex, clientSubmitFuture));
    return new StorageSubmitResults.Success(clientSubmitFuture);
  }

  /** Holds a submission future that cannot be resolved until the associated entry is applied. */
  private record WaitingSubmission(int entryIndex, SettableFuture<String> submissionFuture)
      implements Comparable<WaitingSubmission> {

    @Override
    public int compareTo(WaitingSubmission waitingSubmission) {
      return Integer.compare(this.entryIndex(), waitingSubmission.entryIndex());
    }
  }

  @Override
  public void run() {
    sendHeartbeatToAll();
    while (shouldContinueExecuting) {
      appendEntriesOrSendHeartbeat();
      checkAndUpdateCommitIndex();
      checkAppliedEntriesAndRespondToClients();
    }
    cleanupBeforeTerminating();
  }

  /** Appends {@link Entry}s to any follower who is behind the log; otherwise, a heartbeat. */
  private void appendEntriesOrSendHeartbeat() {
    int lastEntryIndex = raftLog.getLastEntryIndex();
    raftConfiguration
        .getOtherServersInCluster()
        .keySet()
        .forEach(
            raftServerId -> {
              int followerNextIndex = followersNextIndex.get(raftServerId).get();
              if (lastEntryIndex >= followerNextIndex) {
                AppendEntriesRequest request =
                    createAppendEntriesRequest(followerNextIndex, lastEntryIndex);
                sendAppendEntriesToServer(
                    raftServerId, request, followerNextIndex, Optional.of(lastEntryIndex));
              } else {
                AppendEntriesRequest request =
                    createHeartbeatAppendEntriesRequest(followerNextIndex);
                sendAppendEntriesToServer(
                    raftServerId,
                    request,
                    followerNextIndex,
                    /* lastEntryIndex= */ Optional.empty());
              }
            });
  }

  /**
   * Updates the current commit index to the latest log entry that has been replicated to a majority
   * of servers.
   */
  private void checkAndUpdateCommitIndex() {
    int currentCommitIndex = raftVolatileState.getHighestCommittedEntryIndex();
    int currentTerm = raftPersistentState.getCurrentTerm();
    for (int possibleCommitIndex = raftLog.getLastEntryIndex();
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
   * Responds to clients who submitted a {@link RaftCommand} and are waiting for it to be applied.
   */
  private void checkAppliedEntriesAndRespondToClients() {
    int highestAppliedIndex = raftVolatileState.getHighestAppliedEntryIndex();
    for (var waitingSubmission : waitingSubmissions) {
      if (waitingSubmission.entryIndex() > highestAppliedIndex) {
        break;
      }
      waitingSubmission.submissionFuture().set(null);
      waitingSubmissions.remove(waitingSubmission);
    }
  }

  /**
   * Returns an {@link AppendEntriesRequest} based on the provided follower.
   *
   * <p>The request's {@code prevLogIndex} and {@code prevLogTerm} will be the entry preceding the
   * follower's {@code nextIndex}
   *
   * <p>The {@link Entry}s included will be from the follower's {@code nextIndex} to the provided
   * {@code lastEntryIndex} (both inclusive).
   */
  private AppendEntriesRequest createAppendEntriesRequest(
      int followerNextIndex, int lastEntryIndex) {
    ImmutableList<Entry> entries =
        raftLog.getEntriesFromIndex(followerNextIndex, 1 + lastEntryIndex);
    return createBaseAppendEntriesRequest(followerNextIndex - 1).addAllEntries(entries).build();
  }

  /** Sends an {@link AppendEntriesRequest} with no {@link Entry}s to all followers. */
  private void sendHeartbeatToAll() {
    raftConfiguration
        .getOtherServersInCluster()
        .keySet()
        .forEach(
            raftServerId -> {
              int followerNextIndex = followersNextIndex.get(raftServerId).get();
              AppendEntriesRequest request = createHeartbeatAppendEntriesRequest(followerNextIndex);
              sendAppendEntriesToServer(
                  raftServerId, request, followerNextIndex, /* lastEntryIndex= */ Optional.empty());
            });
  }

  /**
   * Returns an {@link AppendEntriesRequest} based on the provided follower with no {@link Entry}s.
   *
   * <p>The request's {@code prevLogIndex} and {@code prevLogTerm} will be the entry preceding the
   * follower's {@code nextIndex}
   */
  private AppendEntriesRequest createHeartbeatAppendEntriesRequest(int followerNextIndex) {
    return createBaseAppendEntriesRequest(followerNextIndex - 1).build();
  }

  /** Sends an {@link AppendEntriesRequest} to a {@link RaftServerId}. */
  private void sendAppendEntriesToServer(
      RaftServerId raftServerId,
      AppendEntriesRequest request,
      int followerNextIndex,
      Optional<Integer> lastEntryIndex) {
    RaftClusterLeaderRpcClient leaderRpcClient =
        raftClusterRpcChannelManager.createRaftClusterLeaderRpcClient();
    ListenableFuture<AppendEntriesResponse> responseFuture =
        leaderRpcClient.appendEntries(raftServerId, request);
    responseFutures.add(responseFuture);

    Futures.addCallback(
        responseFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(AppendEntriesResponse result) {
            responseFutures.remove(responseFuture);
            if (!result.getSuccess() && result.getTerm() > raftPersistentState.getCurrentTerm()) {
              updateTermAndTransitionToFollower(result.getTerm());
            } else if (!result.getSuccess()) {
              lastEntryIndex.ifPresentOrElse(
                  index ->
                      logger.atInfo().log(
                          "Follower [%s] did not accept AppendEntries request with lastEntryIndex [%d] and followerNextIndex [%d].",
                          raftServerId, index, followerNextIndex),
                  () ->
                      logger.atInfo().log(
                          "Follower [%s] did not accept heartbeat AppendEntries request with followerNextIndex [%d]",
                          raftServerId, followerNextIndex));
              followersNextIndex
                  .get(raftServerId)
                  .getAndUpdate(prev -> Math.min(prev, followerNextIndex - 1));
            } else if (lastEntryIndex.isPresent()) {
              logger.atInfo().log(
                  "Follower [%s] accepted lastEntryIndex [%d] with prevLogIndex [%d].",
                  raftServerId, lastEntryIndex, followerNextIndex);
              followersNextIndex
                  .get(raftServerId)
                  .getAndUpdate(prev -> Math.max(prev, followerNextIndex + 1));
              followersMatchIndex
                  .get(raftServerId)
                  .getAndUpdate(prev -> Math.max(prev, lastEntryIndex.get()));
            }
          }

          @Override
          public void onFailure(Throwable t) {
            responseFutures.remove(responseFuture);
            lastEntryIndex.ifPresentOrElse(
                index ->
                    logger.atSevere().withCause(t).log(
                        "AppendEntries request to Follower [%s] with lastEntryIndex [%d] and followerNextIndex [%d] failed",
                        raftServerId, index, followerNextIndex),
                () ->
                    logger.atSevere().withCause(t).log(
                        "Heartbeat AppendEntries request to Follower [%s] with followerNextIndex [%d] failed",
                        raftServerId, followerNextIndex));
          }
        },
        executorService);
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

  /**
   * Cleans up any unresolved client or {@link AppendEntriesResponse} futures before terminating.
   */
  private void cleanupBeforeTerminating() {
    RaftLeaderException exception = new RaftLeaderException("Another leader was discovered");
    for (var waitingSubmission : waitingSubmissions) {
      waitingSubmission.submissionFuture().setException(exception);
      waitingSubmissions.remove(waitingSubmission);
    }
    for (var requestFuture : responseFutures) {
      requestFuture.cancel(true);
      responseFutures.remove(requestFuture);
    }
  }
}
