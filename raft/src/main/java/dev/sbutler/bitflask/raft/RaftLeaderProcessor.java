package dev.sbutler.bitflask.raft;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.rpc.Code;
import com.google.rpc.Status;
import dev.sbutler.bitflask.raft.RaftClusterLeaderRpcClient.AppendEntriesResults;
import dev.sbutler.bitflask.raft.RaftLog.LogEntryDetails;
import dev.sbutler.bitflask.raft.exceptions.RaftException;
import dev.sbutler.bitflask.raft.exceptions.RaftUnknownLeaderException;
import io.grpc.protobuf.StatusProto;
import jakarta.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
  private final RaftClusterConfiguration raftClusterConfiguration;

  private final ConcurrentMap<RaftServerId, Integer> followersNextIndex = new ConcurrentHashMap<>();

  @Inject
  RaftLeaderProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      ListeningExecutorService executorService,
      RaftLog raftLog,
      RaftClusterRpcChannelManager raftClusterRpcChannelManager,
      RaftCommandConverter raftCommandConverter,
      RaftClusterConfiguration raftClusterConfiguration) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.executorService = executorService;
    this.raftLog = raftLog;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
    this.raftCommandConverter = raftCommandConverter;
    this.raftClusterConfiguration = raftClusterConfiguration;

    int nextIndex = raftLog.getLastEntryIndex() + 1;
    for (var followerServerId : raftClusterConfiguration.getOtherServersInCluster().keySet()) {
      followersNextIndex.put(followerServerId, nextIndex);
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
  public void run() {}

  @Override
  public RaftSubmitResults submitCommand(RaftCommand raftCommand) {
    Entry newEntry;
    try {
      newEntry = raftCommandConverter.convert(raftCommand);
      raftLog.appendEntry(newEntry);
    } catch (RaftException e) {
      logger.atSevere().withCause(e).log("Unable to create or append entry to log");
      return new RaftSubmitResults.Success(immediateFailedFuture(e));
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Unable to create or append entry to log");
      return new RaftSubmitResults.Success(
          immediateFailedFuture(new RaftException("Unknown error while submitting.")));
    }

    SettableFuture<Void> clientSubmitFuture = SettableFuture.create();

    ListenableFuture<Void> leaderSubmitFuture =
        executorService.submit(
            () -> {
              AppendEntriesRequest request =
                  createBaseAppendEntriesRequest().addEntries(newEntry).build();
              try (RaftClusterLeaderRpcClient leaderRpcClient =
                  raftClusterRpcChannelManager.createRaftClusterLeaderRpcClient()) {
                leaderRpcClient.appendEntries(request);

                AppendEntriesResults results = leaderRpcClient.getCurrentAppendEntriesResults();
                while (!results.majorityResponsesSuccessful()) {
                  results = leaderRpcClient.getCurrentAppendEntriesResults();
                }

                while (!results.allResponsesReceived()) {
                  results = leaderRpcClient.getCurrentAppendEntriesResults();
                }
                if (!results.allRequestsSuccessful()) {
                  // TODO: handle failed responses
                }
              }
              return null;
            });
    return new RaftSubmitResults.Success(clientSubmitFuture);
  }

  /**
   * Creates an {@link dev.sbutler.bitflask.raft.AppendEntriesRequest.Builder} without the {@link
   * dev.sbutler.bitflask.raft.Entry} list populated.
   */
  private AppendEntriesRequest.Builder createBaseAppendEntriesRequest() {
    String leaderId =
        raftVolatileState
            .getLeaderServerId()
            .map(RaftServerId::id)
            .orElseThrow(
                () ->
                    new RaftUnknownLeaderException(
                        "LeaderServerId was not set for use by the RaftLeaderProcessor"));
    LogEntryDetails lastLogEntryDetails = raftLog.getLastLogEntryDetails();

    return AppendEntriesRequest.newBuilder()
        .setTerm(raftPersistentState.getCurrentTerm())
        .setLeaderId(leaderId)
        .setPrevLogTerm(lastLogEntryDetails.term())
        .setPrevLogIndex(lastLogEntryDetails.index());
  }
}
