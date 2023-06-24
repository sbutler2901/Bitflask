package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.raft.RaftGrpc.RaftFutureStub;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Utility class for handling rpc calls used by the {@link RaftCandidateProcessor}. */
final class RaftClusterCandidateRpcClient {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  private final AtomicInteger responsesReceived = new AtomicInteger(0);
  private final AtomicInteger votesReceived = new AtomicInteger(0);
  private final AtomicLong largestTermSeen = new AtomicLong(-1);

  private ImmutableList<ListenableFuture<RequestVoteResponse>> responseFutures = ImmutableList.of();

  RaftClusterCandidateRpcClient(
      ListeningExecutorService executorService,
      ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs) {
    this.executorService = executorService;
    this.otherServerStubs = otherServerStubs;
  }

  void requestVotes(RequestVoteRequest request) {
    ImmutableList.Builder<ListenableFuture<RequestVoteResponse>> responseFuturesBuilder =
        ImmutableList.builder();
    for (var stubsEntry : otherServerStubs.entrySet()) {
      ListenableFuture<RequestVoteResponse> responseFuture =
          stubsEntry.getValue().requestVote(request);
      Futures.addCallback(
          responseFuture, new RequestVoteFutureCallback(stubsEntry.getKey()), executorService);
    }
    responseFutures = responseFuturesBuilder.build();
  }

  int getNumberRequestsSent() {
    return responseFutures.size();
  }

  int getNumberResponsesReceived() {
    return responsesReceived.get();
  }

  int getNumberVotesReceived() {
    return votesReceived.get();
  }

  long getLargestTermSeen() {
    return largestTermSeen.get();
  }

  /** Cancels all pending requests, if any. */
  void cancelRequests() {
    responseFutures.forEach(future -> future.cancel(true));
  }

  private final class RequestVoteFutureCallback implements FutureCallback<RequestVoteResponse> {
    private final RaftServerId calledRaftServerId;

    private RequestVoteFutureCallback(RaftServerId calledRaftServerId) {
      this.calledRaftServerId = calledRaftServerId;
    }

    @Override
    public void onSuccess(RequestVoteResponse result) {
      responsesReceived.getAndIncrement();
      largestTermSeen.getAndUpdate(current -> Math.max(current, result.getTerm()));
      if (result.getVoteGranted()) {
        votesReceived.getAndIncrement();
        logger.atInfo().log("Vote granted by [%s]", calledRaftServerId);
      } else {
        logger.atInfo().log("Voted denied by [%s]", calledRaftServerId);
      }
    }

    @Override
    public void onFailure(Throwable t) {
      responsesReceived.getAndIncrement();
      logger.atWarning().withCause(t).log("Error received from [%s]", calledRaftServerId);
    }
  }
}
