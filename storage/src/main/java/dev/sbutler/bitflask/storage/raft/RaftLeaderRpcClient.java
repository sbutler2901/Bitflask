package dev.sbutler.bitflask.storage.raft;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.raft.RaftGrpc.RaftFutureStub;
import jakarta.inject.Inject;

/**
 * Utility class for handling rpc calls for the {@link RaftLeaderProcessor}.
 *
 * <p>An instances should be created with each new RaftLeaderProcessor.
 */
final class RaftLeaderRpcClient {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftLeaderState raftLeaderState;
  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  @Inject
  RaftLeaderRpcClient(
      RaftRpcChannelManager rpcChannelManager, @Assisted RaftLeaderState raftLeaderState) {
    this.otherServerStubs = rpcChannelManager.getOtherServerStubs();
    this.raftLeaderState = raftLeaderState;
  }

  interface Factory {
    RaftLeaderRpcClient create(RaftLeaderState raftLeaderState);
  }

  ListenableFuture<AppendEntriesResponse> appendEntries(
      RaftServerId raftServerId, AppendEntriesRequest request) {
    return otherServerStubs.get(raftServerId).appendEntries(request);
  }
}
