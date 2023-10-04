package dev.sbutler.bitflask.storage.raft;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.raft.RaftGrpc.RaftFutureStub;
import jakarta.inject.Inject;

/** Utility class for handling rpc calls used by the {@link RaftLeaderProcessor}. */
final class RaftLeaderRpcClient {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  @Inject
  RaftLeaderRpcClient(@Assisted ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs) {
    this.otherServerStubs = otherServerStubs;
  }

  interface Factory {
    RaftLeaderRpcClient create(ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs);
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
