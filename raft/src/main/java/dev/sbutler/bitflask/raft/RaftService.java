package dev.sbutler.bitflask.raft;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import jakarta.inject.Inject;

/** The service implementing Raft RPC endpoints. */
final class RaftService extends RaftGrpc.RaftImplBase {

  private final RaftModeManager raftModeManager;

  @Inject
  RaftService(RaftModeManager raftModeManager) {
    this.raftModeManager = raftModeManager;
  }

  @Override
  public void requestVote(
      RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
    try {
      RequestVoteResponse response =
          raftModeManager.getRaftStateProcessor().processRequestVoteRequest(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Exception e) {
      StatusRuntimeException statusRuntimeException =
          StatusProto.toStatusRuntimeException(
              Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build());
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void appendEntries(
      AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
    try {
      AppendEntriesResponse response =
          raftModeManager.getRaftStateProcessor().processAppendEntriesRequest(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Exception e) {
      StatusRuntimeException statusRuntimeException =
          StatusProto.toStatusRuntimeException(
              Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build());
      responseObserver.onError(statusRuntimeException);
    }
  }
}
