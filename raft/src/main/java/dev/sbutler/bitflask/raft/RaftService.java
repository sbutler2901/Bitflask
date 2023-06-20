package dev.sbutler.bitflask.raft;

import io.grpc.stub.StreamObserver;
import jakarta.inject.Inject;

/** The service implementing Raft RPC endpoints. */
final class RaftService extends RaftGrpc.RaftImplBase {

  @Inject
  RaftService() {}

  @Override
  public void requestVote(
      RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
    RequestVoteResponse response = RequestVoteResponse.getDefaultInstance();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void appendEntries(
      AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
    AppendEntriesResponse response = AppendEntriesResponse.getDefaultInstance();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
