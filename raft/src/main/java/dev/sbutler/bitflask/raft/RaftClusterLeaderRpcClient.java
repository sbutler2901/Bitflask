package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.raft.RaftGrpc.RaftFutureStub;

/** Utility class for handling rpc calls used by the {@link RaftLeaderProcessor}. */
final class RaftClusterLeaderRpcClient {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  RaftClusterLeaderRpcClient(
      ListeningExecutorService executorService,
      ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs) {
    this.executorService = executorService;
    this.otherServerStubs = otherServerStubs;
  }
}
