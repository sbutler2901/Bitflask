package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.rpc.Code;
import com.google.rpc.Status;
import dev.sbutler.bitflask.raft.exceptions.RaftLeaderException;
import dev.sbutler.bitflask.raft.exceptions.RaftUnknownLeaderException;
import io.grpc.protobuf.StatusProto;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
  private final RaftCommandTopic raftCommandTopic;
  private final RaftClusterConfiguration raftClusterConfiguration;

  private final ConcurrentMap<RaftServerId, AtomicInteger> followersNextIndex =
      new ConcurrentHashMap<>();
  private final ConcurrentNavigableMap<Integer, SubmittedEntryState> submittedEntryStateMap =
      new ConcurrentSkipListMap<>();

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
      RaftCommandTopic raftCommandTopic,
      RaftClusterConfiguration raftClusterConfiguration) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.executorService = executorService;
    this.raftLog = raftLog;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
    this.raftCommandConverter = raftCommandConverter;
    this.raftCommandTopic = raftCommandTopic;
    this.raftClusterConfiguration = raftClusterConfiguration;

    int nextIndex = raftLog.getLastEntryIndex() + 1;
    for (var followerServerId : raftClusterConfiguration.getOtherServersInCluster().keySet()) {
      followersNextIndex.put(followerServerId, new AtomicInteger(nextIndex));
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
  public RaftSubmitResults submitCommand(RaftCommand raftCommand) {
    Entry newEntry = raftCommandConverter.convert(raftCommand);
    int newEntryIndex = raftLog.appendEntry(newEntry);
    SettableFuture<Void> clientSubmitFuture = SettableFuture.create();
    submittedEntryStateMap.put(
        newEntryIndex,
        new SubmittedEntryState(newEntry, new CopyOnWriteArrayList<>(), clientSubmitFuture));
    return new RaftSubmitResults.Success(clientSubmitFuture);
  }

  /**
   * Holds state relevant to an {@link Entry} that has been submitted by a client and waiting to be
   * committed.
   */
  private record SubmittedEntryState(
      Entry entry,
      CopyOnWriteArrayList<RaftServerId> successResponseServers,
      SettableFuture<Void> clientSubmitFuture) {
    private void successfulServerAddIfAbsent(RaftServerId raftServerId) {
      successResponseServers.addIfAbsent(raftServerId);
    }

    private int numberOfSuccessfulServers() {
      return successResponseServers.size();
    }
  }

  @Override
  public void run() {
    sendHeartbeatToAll();
    while (shouldContinueExecuting) {
      checkSubmittedEntryState();
      if (raftVolatileState.committedEntriesNeedApplying()) {
        applyCommittedEntries();
      }
      checkAppliedEntriesAndRespondToClients();
      if (logHasUncommittedEntries()) {
        sendAppendEntriesToAll(raftLog.getLastEntryIndex());
      } else {
        sendHeartbeatToAll();
      }
    }

    RaftLeaderException exception = new RaftLeaderException("Another leader was discovered");
    for (var submittedEntryState : submittedEntryStateMap.values()) {
      submittedEntryState.clientSubmitFuture().setException(exception);
    }
    // TODO: clean unresolved futures
  }

  /**
   * Commits {@link Entry}s that have been submitted by clients if a majority of success responses
   * has been received.
   */
  private void checkSubmittedEntryState() {
    for (var submittedEntry : submittedEntryStateMap.entrySet()) {
      int numServersReplicated = 1 + submittedEntry.getValue().numberOfSuccessfulServers();
      double halfOfServers = raftClusterConfiguration.clusterServers().size() / 2.0;
      if (numServersReplicated <= halfOfServers) {
        break;
      }
      raftVolatileState.setHighestCommittedEntryIndex(submittedEntry.getKey());
    }
  }

  /** Applies any committed entries that have not been applied yet. */
  private void applyCommittedEntries() {
    int highestAppliedIndex = raftVolatileState.getHighestAppliedEntryIndex();
    int highestCommittedIndex = raftVolatileState.getHighestCommittedEntryIndex();
    for (int nextIndex = highestAppliedIndex + 1; nextIndex <= highestCommittedIndex; nextIndex++) {
      try {
        raftCommandTopic.notifyObservers(
            raftCommandConverter.reverse().convert(raftLog.getEntryAtIndex(nextIndex)));
        raftVolatileState.setHighestAppliedEntryIndex(nextIndex);
      } catch (Exception e) {
        logger.atSevere().withCause(e).log("Failed to apply command at index [%d].", nextIndex);
        // TODO: terminate server if unrecoverable?
        break;
      }
    }
  }

  private boolean logHasUncommittedEntries() {
    return raftVolatileState.getHighestCommittedEntryIndex() < raftLog.getLastEntryIndex();
  }

  /**
   * Responds to clients who submitted a {@link RaftCommand} and are waiting for it to be applied.
   */
  private void checkAppliedEntriesAndRespondToClients() {
    int highestAppliedIndex = raftVolatileState.getHighestAppliedEntryIndex();
    for (var submittedEntry : submittedEntryStateMap.entrySet()) {
      if (submittedEntry.getKey() > highestAppliedIndex) {
        break;
      }
      submittedEntry.getValue().clientSubmitFuture().set(null);
      submittedEntryStateMap.remove(submittedEntry.getKey());
    }
  }

  /**
   * Sends an {@link AppendEntriesRequest} with {@link Entry}s ranging from the follower's {@code
   * nextIndex} to the {@code lastEntryIndex} (both-inclusive) to all followers.
   */
  private void sendAppendEntriesToAll(int lastEntryIndex) {
    raftClusterConfiguration
        .getOtherServersInCluster()
        .keySet()
        .forEach(
            raftServerId -> {
              int followerNextIndex = followersNextIndex.get(raftServerId).get();
              AppendEntriesRequest request =
                  createAppendEntriesRequest(followerNextIndex, lastEntryIndex);
              sendAppendEntriesToServer(
                  raftServerId, request, followerNextIndex, Optional.of(lastEntryIndex));
            });
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
    raftClusterConfiguration
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

    Futures.addCallback(
        responseFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(AppendEntriesResponse result) {
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
                  .compareAndExchange(followerNextIndex, followerNextIndex - 1);
            } else if (lastEntryIndex.isPresent()) {
              logger.atInfo().log(
                  "Follower [%s] accepted lastEntryIndex [%d] with prevLogIndex [%d].",
                  raftServerId, lastEntryIndex, followerNextIndex);
              for (var submittedEntry : submittedEntryStateMap.entrySet()) {
                if (submittedEntry.getKey() > lastEntryIndex.get()) {
                  break;
                }
                submittedEntry.getValue().successfulServerAddIfAbsent(raftServerId);
              }
            }
          }

          @Override
          public void onFailure(Throwable t) {
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
   * Creates an {@link dev.sbutler.bitflask.raft.AppendEntriesRequest.Builder} without the {@link
   * dev.sbutler.bitflask.raft.Entry} list populated.
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
}
