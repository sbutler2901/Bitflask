package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static dev.sbutler.bitflask.storage.raft.RaftLeaderProcessor.AppendEntriesSubmission;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.raft.RaftGrpc.RaftFutureStub;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftUnknownLeaderException;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Utility class for handling rpc calls for the {@link RaftLeaderProcessor}.
 *
 * <p>An instances should be created with each new RaftLeaderProcessor.
 */
final class RaftLeaderRpcClient {

  private static final double REQUEST_TIMEOUT_RATIO = 0.75;

  private final ScheduledExecutorService executorService;
  private final RaftConfiguration raftConfiguration;
  private final RaftPersistentState raftPersistentState;
  private final RaftVolatileState raftVolatileState;
  private final RaftLeaderState raftLeaderState;
  private final RaftLog raftLog;
  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  private final Duration requestTimeoutDuration;

  @Inject
  RaftLeaderRpcClient(
      RaftRpcChannelManager rpcChannelManager,
      @RaftLeaderListeningScheduledExecutorService ScheduledExecutorService executorService,
      RaftConfiguration raftConfiguration,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftLog raftLog,
      @Assisted RaftLeaderState raftLeaderState) {
    this.executorService = executorService;
    this.raftConfiguration = raftConfiguration;
    this.raftPersistentState = raftPersistentState;
    this.raftVolatileState = raftVolatileState;
    this.raftLog = raftLog;
    this.raftLeaderState = raftLeaderState;
    this.otherServerStubs = rpcChannelManager.getOtherServerStubs();

    requestTimeoutDuration =
        Duration.ofMillis(
            Math.round(
                raftConfiguration.raftTimerInterval().minimumMilliSeconds()
                    * REQUEST_TIMEOUT_RATIO));
  }

  interface Factory {
    RaftLeaderRpcClient create(RaftLeaderState raftLeaderState);
  }

  /** Sends an {@link AppendEntriesRequest} with no {@link Entry}s to all followers. */
  ImmutableList<AppendEntriesSubmission> broadcastHeartbeat() {
    return raftConfiguration.getOtherServersInCluster().keySet().stream()
        .map(
            raftServerId -> {
              int nextIndex = raftLeaderState.getFollowerNextIndex(raftServerId);
              return submitAppendEntriesToServer(raftServerId, nextIndex, nextIndex);
            })
        .collect(toImmutableList());
  }

  /** Appends {@link Entry}s to any follower who is behind the log; otherwise, a heartbeat. */
  ImmutableList<AppendEntriesSubmission> broadcastAppendEntriesOrHeartbeat() {
    int lastEntryIndex = raftLog.getLastLogEntryDetails().index();
    return raftConfiguration.getOtherServersInCluster().keySet().stream()
        .map(
            raftServerId ->
                submitAppendEntriesToServer(
                    raftServerId,
                    raftLeaderState.getFollowerNextIndex(raftServerId),
                    1 + lastEntryIndex))
        .collect(toImmutableList());
  }

  /**
   * Submits an {@link AppendEntriesRequest} and requires that a response is received within a
   * deadline, or cancels it.
   */
  private AppendEntriesSubmission submitAppendEntriesToServer(
      RaftServerId serverId, int followerNextIndex, int lastEntryIndex) {
    ImmutableList<Entry> entries = raftLog.getEntriesFromIndex(followerNextIndex, lastEntryIndex);
    AppendEntriesRequest request =
        createBaseAppendEntriesRequest(followerNextIndex - 1).addAllEntries(entries).build();
    ListenableFuture<AppendEntriesResponse> responseFuture =
        Futures.withTimeout(
            otherServerStubs.get(serverId).appendEntries(request),
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
}
