package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.raft.RaftGrpc.RaftFutureStub;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Utility class for handling rpc calls used by the {@link RaftLeaderProcessor}. */
final class RaftClusterLeaderRpcClient implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  private final AtomicInteger responsesReceived = new AtomicInteger(0);
  private final AtomicInteger successfulResponses = new AtomicInteger(0);
  private final AtomicInteger largestTermSeen = new AtomicInteger(-1);

  private ImmutableList<ListenableFuture<AppendEntriesResponse>> responseFutures =
      ImmutableList.of();

  RaftClusterLeaderRpcClient(
      ListeningExecutorService executorService,
      ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs) {
    this.executorService = executorService;
    this.otherServerStubs = otherServerStubs;
  }

  void appendEntries(AppendEntriesRequest request) {
    ImmutableList.Builder<ListenableFuture<AppendEntriesResponse>> responseFuturesBuilder =
        ImmutableList.builder();
    for (var stubsEntry : otherServerStubs.entrySet()) {
      ListenableFuture<AppendEntriesResponse> responseFuture =
          stubsEntry.getValue().appendEntries(request);
      Futures.addCallback(
          responseFuture, new AppendEntriesFutureCallback(stubsEntry.getKey()), executorService);
    }
    responseFutures = responseFuturesBuilder.build();
  }

  /**
   * Gets the current results of all AppendEntries RPCs.
   *
   * <p>The returned {@link AppendEntriesResults} will vary until all responses have been received.
   */
  AppendEntriesResults getCurrentAppendEntriesResults() {
    return new AppendEntriesResults(
        responseFutures.size(),
        responsesReceived.get(),
        successfulResponses.getAndIncrement(),
        largestTermSeen.get());
  }

  /** Cancels all pending requests, if any. */
  @Override
  public void close() {
    responseFutures.stream()
        .filter(Predicate.not(Future::isDone))
        .filter(Predicate.not(Future::isCancelled))
        .forEach(future -> future.cancel(true));
  }

  /**
   * A simplified snapshot of all of {@link AppendEntriesResponse}s received from other servers in
   * the cluster.
   */
  record AppendEntriesResults(
      int numberRequestsSent,
      int numberResponsesReceived,
      int numberSuccessfulResponses,
      int largestTermSeen) {

    boolean allResponsesReceived() {
      return numberRequestsSent() == numberResponsesReceived();
    }

    boolean majorityResponsesSuccessful() {
      double requiredForMajority = numberRequestsSent() / 2.0;
      return numberSuccessfulResponses() > requiredForMajority;
    }

    boolean allRequestsSuccessful() {
      return numberRequestsSent() == numberSuccessfulResponses();
    }
  }

  private final class AppendEntriesFutureCallback implements FutureCallback<AppendEntriesResponse> {

    private final RaftServerId calledRaftServerId;

    private AppendEntriesFutureCallback(RaftServerId calledRaftServerId) {
      this.calledRaftServerId = calledRaftServerId;
    }

    @Override
    public void onSuccess(AppendEntriesResponse result) {
      responsesReceived.getAndIncrement();
      largestTermSeen.getAndUpdate(current -> Math.max(current, result.getTerm()));
      if (result.getSuccess()) {
        successfulResponses.getAndIncrement();
        logger.atInfo().log("Success received by [%s]", calledRaftServerId);
      } else {
        logger.atInfo().log("Failed received by [%s]", calledRaftServerId);
      }
    }

    @Override
    public void onFailure(Throwable t) {
      //      responsesReceived.getAndIncrement();
      logger.atWarning().withCause(t).log("Error received from [%s]", calledRaftServerId);
      // TODO: handle infinite retry
    }
  }
}
