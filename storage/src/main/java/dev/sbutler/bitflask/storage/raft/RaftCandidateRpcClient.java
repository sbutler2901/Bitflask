package dev.sbutler.bitflask.storage.raft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.raft.RaftGrpc.RaftFutureStub;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

/**
 * Utility class for handling rpc calls used by the {@link RaftCandidateProcessor}.
 *
 * <p>Note: this class should only be used for a single election cycle and used in a try/with.
 */
final class RaftCandidateRpcClient implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  private final AtomicInteger responsesReceived = new AtomicInteger(0);
  private final AtomicInteger votesReceived = new AtomicInteger(0);
  private final AtomicInteger largestTermSeen = new AtomicInteger(0);

  private ImmutableList<ListenableFuture<RequestVoteResponse>> responseFutures = ImmutableList.of();

  @Inject
  RaftCandidateRpcClient(
      ListeningExecutorService executorService,
      @Assisted ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs) {
    this.executorService = executorService;
    this.otherServerStubs = otherServerStubs;
  }

  interface Factory {
    RaftCandidateRpcClient create(ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs);
  }

  /**
   * In parallel, asynchronously sends the {@link RequestVoteRequest} to all other servers in the
   * Raft cluster.
   *
   * <p>The results of these calls can be polled by calling {@link
   * RaftCandidateRpcClient#getCurrentRequestVotesResults()};
   */
  void requestVotes(RequestVoteRequest request) {
    largestTermSeen.set(request.getTerm());
    logger.atInfo().log("Requesting votes for term [%d].", request.getTerm());
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

  /**
   * Gets the current results of all RequestVotes RPCs.
   *
   * <p>The returned {@link RequestVotesResults} will vary until all responses have been received.
   */
  RequestVotesResults getCurrentRequestVotesResults() {
    return new RequestVotesResults(
        otherServerStubs.size(),
        responsesReceived.get(),
        votesReceived.get(),
        largestTermSeen.get());
  }

  /** Cancels all pending requests, if any. */
  @Override
  public void close() {
    responseFutures.forEach(future -> future.cancel(true));
  }

  /**
   * A simplified snapshot of all of {@link RequestVoteResponse}s received from other servers in the
   * clusters.
   */
  record RequestVotesResults(
      int numOtherServers, int numResponsesReceived, int numVotesReceived, int largestTermSeen) {

    boolean allResponsesReceived() {
      return numOtherServers() == numResponsesReceived();
    }

    boolean receivedMajorityVotes() {
      // include vote for self
      int votesReceived = 1 + numVotesReceived();
      double halfRequiredRequests = numOtherServers() / 2.0;
      return votesReceived > halfRequiredRequests;
    }
  }

  /** Handles a single cluster's {@link RequestVoteResponse}. */
  private final class RequestVoteFutureCallback implements FutureCallback<RequestVoteResponse> {
    private final RaftServerId calledRaftServerId;

    private RequestVoteFutureCallback(RaftServerId calledRaftServerId) {
      this.calledRaftServerId = calledRaftServerId;
    }

    @Override
    public void onSuccess(RequestVoteResponse result) {
      int prevLargestTerm =
          largestTermSeen.getAndUpdate(current -> Math.max(current, result.getTerm()));
      if (result.getVoteGranted()) {
        votesReceived.getAndIncrement();
        logger.atInfo().log(
            "Vote granted by [%s] with term [%d], prevLargestTerm [%d].",
            calledRaftServerId.id(), result.getTerm(), prevLargestTerm);
      } else {
        logger.atInfo().log(
            "Voted denied by [%s] with term [%d], prevLargestTerm [%d].",
            calledRaftServerId.id(), result.getTerm(), prevLargestTerm);
      }
      responsesReceived.getAndIncrement();
    }

    @Override
    public void onFailure(@Nonnull Throwable t) {
      if (t instanceof StatusRuntimeException e
          && Status.UNAVAILABLE.getCode().equals(e.getStatus().getCode())) {
        logger.atWarning().atMostEvery(5, TimeUnit.SECONDS).log(
            "Server [%s] unavailable for RequestVote RPC.", calledRaftServerId.id());
      } else {
        logger.atWarning().withCause(t).log("Error received from [%s]", calledRaftServerId.id());
      }
      responsesReceived.getAndIncrement();
    }
  }
}
