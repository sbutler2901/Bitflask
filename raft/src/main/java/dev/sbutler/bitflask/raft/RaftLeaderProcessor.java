package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

  private final ConcurrentMap<RaftServerId, AtomicInteger> followersNextIndex =
      new ConcurrentHashMap<>();
  private final ConcurrentNavigableMap<Integer, SettableFuture<Void>> clientResponseMap =
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
  public void handleElectionTimeout() {
    throw new IllegalStateException(
        "Raft in LEADER mode should not have an election timer running");
  }

  @Override
  public void run() {
    sendHeartbeats();
    while (shouldContinueExecuting) {
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
    for (var clientResponseEntry : clientResponseMap.entrySet()) {
      if (clientResponseEntry.getKey() > highestAppliedIndex) {
        break;
      }
      clientResponseEntry.getValue().set(null);
      clientResponseMap.remove(clientResponseEntry.getKey());
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

  @Override
  public RaftSubmitResults submitCommand(RaftCommand raftCommand) {
    Entry newEntry = raftCommandConverter.convert(raftCommand);
    int newEntryIndex = raftLog.appendEntry(newEntry);
    SettableFuture<Void> clientSubmitFuture = SettableFuture.create();
    clientResponseMap.put(newEntryIndex, clientSubmitFuture);
    return new RaftSubmitResults.Success(clientSubmitFuture);
  }

  private ImmutableMap<RaftServerId, ListenableFuture<Void>> sentRetryingAppendEntriesToAll(
      int lastEntryLogIndex) {
    ImmutableMap.Builder<RaftServerId, ListenableFuture<Void>> responseFutures =
        ImmutableMap.builder();
    for (var key : followersNextIndex.keySet()) {
      responseFutures.put(key, sendRetryingAppendEntries(key, lastEntryLogIndex));
    }
    return responseFutures.build();
  }

  private ListenableFuture<Void> sendRetryingAppendEntries(
      RaftServerId raftServerId, int lastEntryLogIndex) {
    SettableFuture<Void> requestCompleted = SettableFuture.create();
    sendRetryingAppendEntries(raftServerId, lastEntryLogIndex, requestCompleted);
    return requestCompleted;
  }

  private void sendRetryingAppendEntries(
      RaftServerId raftServerId, int lastEntryIndex, SettableFuture<Void> requestCompleted) {
    int followerNextIndex = followersNextIndex.get(raftServerId).get();
    ImmutableList<Entry> entries =
        raftLog.getEntriesFromIndex(followerNextIndex, 1 + lastEntryIndex);
    RaftClusterLeaderRpcClient leaderRpcClient =
        raftClusterRpcChannelManager.createRaftClusterLeaderRpcClient();
    AppendEntriesRequest request =
        createBaseAppendEntriesRequest(followerNextIndex).addAllEntries(entries).build();
    Futures.addCallback(
        leaderRpcClient.appendEntries(raftServerId, request),
        new FutureCallback<>() {
          @Override
          public void onSuccess(AppendEntriesResponse result) {
            if (!result.getSuccess() && result.getTerm() > raftPersistentState.getCurrentTerm()) {
              handleLargerTermFound(result.getTerm());
            } else if (!result.getSuccess()) {
              logger.atInfo().log(
                  "Follower [%s] did not accept index [%d] with prevLogIndex [%d]. Trying again with lower prevLogIndex.",
                  raftServerId, lastEntryIndex, followerNextIndex);
              followersNextIndex
                  .get(raftServerId)
                  .compareAndExchange(followerNextIndex, followerNextIndex - 1);
              sendRetryingAppendEntries(raftServerId, lastEntryIndex);
            } else {
              requestCompleted.set(null);
            }
          }

          @Override
          public void onFailure(Throwable t) {
            String msg =
                String.format(
                    "Follower [%s] AppendEntries request failed for Entry at index [%d] with prevLogIndex [%d]",
                    raftServerId, lastEntryIndex, followerNextIndex);
            // TODO: try again?
            requestCompleted.setException(new RaftLeaderException(msg, t));
          }
        },
        executorService);
  }

  private void handleLargerTermFound(int term) {
    logger.atWarning().log("Larger term [%d] found transitioning to follower.", term);
    shouldContinueExecuting = false;
    raftPersistentState.setCurrentTermAndResetVote(term);
    RaftLeaderException exception = new RaftLeaderException("Another leader was discovered");
    for (var clientResponseFuture : clientResponseMap.values()) {
      clientResponseFuture.setException(exception);
    }
    raftModeManager.transitionToFollowerState();
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
