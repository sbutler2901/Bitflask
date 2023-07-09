package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.raft.RaftGrpc.RaftFutureStub;

/** Utility class for handling rpc calls used by the {@link RaftLeaderProcessor}. */
final class RaftClusterLeaderRpcClient {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  RaftClusterLeaderRpcClient(ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs) {
    this.otherServerStubs = otherServerStubs;
  }

  ImmutableMap<RaftServerId, ListenableFuture<AppendEntriesResponse>> appendEntries(
      AppendEntriesRequest request) {
    ImmutableMap.Builder<RaftServerId, ListenableFuture<AppendEntriesResponse>> futuresMap =
        ImmutableMap.builder();
    for (var raftServerId : otherServerStubs.keySet()) {
      ListenableFuture<AppendEntriesResponse> responseFuture = appendEntries(raftServerId, request);
      futuresMap.put(raftServerId, responseFuture);
    }
    return futuresMap.build();
  }

  ListenableFuture<AppendEntriesResponse> appendEntries(
      RaftServerId raftServerId, AppendEntriesRequest request) {
    return otherServerStubs.get(raftServerId).appendEntries(request);
  }
}
