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
  public void run() {
    sendHeartbeats();
    while (shouldContinueExecuting) {
      checkSubmittedEntryState();
      if (raftVolatileState.committedEntriesNeedApplying()) {
        applyCommittedEntries();
      }
      checkAppliedEntriesAndRespondToClients();
      if (logHasUncommittedEntries()) {
        commitEntries();
      } else {
        sendHeartbeats();
      }
    }

    RaftLeaderException exception = new RaftLeaderException("Another leader was discovered");
    for (var submittedEntryState : submittedEntryStateMap.values()) {
      submittedEntryState.clientSubmitFuture().setException(exception);
    }
    // TODO: clean unresolved futures
  }

  private void commitEntries() {
    // TODO: commit entries in log within current term
  }

  private void sendHeartbeats() {
    // TODO: send heartbeats to all servers.
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

  private void sentAppendEntriesToAll(
      AppendEntriesRequest request, int followerNextIndex, int lastEntryIndex) {
    raftClusterConfiguration
        .getOtherServersInCluster()
        .keySet()
        .forEach(
            raftServerId ->
                sendAppendEntriesToServer(
                    raftServerId, request, followerNextIndex, lastEntryIndex));
  }

  private void sendAppendEntriesToServer(
      RaftServerId raftServerId,
      AppendEntriesRequest request,
      int followerNextIndex,
      int lastEntryIndex) {
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
              logger.atInfo().log(
                  "Follower [%s] did not accept lastEntryIndex [%d] with prevLogIndex [%d].",
                  raftServerId, lastEntryIndex, followerNextIndex);
              followersNextIndex
                  .get(raftServerId)
                  .compareAndExchange(followerNextIndex, followerNextIndex - 1);
            } else {
              logger.atInfo().log(
                  "Follower [%s] accepted lastEntryIndex [%d] with prevLogIndex [%d].",
                  raftServerId, lastEntryIndex, followerNextIndex);
              for (var submittedEntry : submittedEntryStateMap.entrySet()) {
                if (submittedEntry.getKey() > lastEntryIndex) {
                  break;
                }
                submittedEntry.getValue().successfulServerAddIfAbsent(raftServerId);
              }
            }
          }

          @Override
          public void onFailure(Throwable t) {
            logger.atSevere().withCause(t).log(
                "Follower [%s] AppendEntries request failed for Entry at index [%d] with prevLogIndex [%d]",
                raftServerId, lastEntryIndex, followerNextIndex);
          }
        },
        executorService);
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
  private AppendEntriesRequest getAppendEntriesRequestForFollower(
      RaftServerId raftServerId, int lastEntryIndex) {
    int followerNextIndex = followersNextIndex.get(raftServerId).get();
    ImmutableList<Entry> entries =
        raftLog.getEntriesFromIndex(followerNextIndex, 1 + lastEntryIndex);
    return createBaseAppendEntriesRequest(followerNextIndex - 1).addAllEntries(entries).build();
  }

  /**
   * Returns an {@link AppendEntriesRequest} based on the provided follower with no {@link Entry}s.
   *
   * <p>The request's {@code prevLogIndex} and {@code prevLogTerm} will be the entry preceding the
   * follower's {@code nextIndex}
   */
  private AppendEntriesRequest getHeartbeatAppendEntriesRequestForFollower(
      RaftServerId raftServerId) {
    int followerNextIndex = followersNextIndex.get(raftServerId).get();
    return createBaseAppendEntriesRequest(followerNextIndex - 1).build();
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
