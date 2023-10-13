package dev.sbutler.bitflask.storage.raft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

/** Tests for {@link RaftRpcService}. */
public class RaftRpcServiceTest {

  private final RaftModeManager modeManager = mock(RaftModeManager.class);

  private final RaftRpcService rpcService = new RaftRpcService(modeManager);

  @Test
  public void requestVote_success() {
    StreamObserver<RequestVoteResponse> responseObserver = mock(StreamObserver.class);
    var response = RequestVoteResponse.getDefaultInstance();
    when(modeManager.processRequestVoteRequest(any())).thenReturn(response);

    rpcService.requestVote(RequestVoteRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onNext(response);
    verify(responseObserver, times(1)).onCompleted();
  }

  @Test
  public void requestVote_statusRuntimeExceptionThrown() {
    StreamObserver<RequestVoteResponse> responseObserver = mock(StreamObserver.class);
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(modeManager.processRequestVoteRequest(any())).thenThrow(exception);

    rpcService.requestVote(RequestVoteRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onError(exception);
  }

  @Test
  public void requestVote_runtimeExceptionThrown() {
    StreamObserver<RequestVoteResponse> responseObserver = mock(StreamObserver.class);
    RuntimeException exception = new RuntimeException("test");
    when(modeManager.processRequestVoteRequest(any())).thenThrow(exception);

    rpcService.requestVote(RequestVoteRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onError(any(StatusRuntimeException.class));
  }

  @Test
  public void requestVote_runtimeExceptionWithoutMessageThrown() {
    StreamObserver<RequestVoteResponse> responseObserver = mock(StreamObserver.class);
    RuntimeException exception = new RuntimeException();
    when(modeManager.processRequestVoteRequest(any())).thenThrow(exception);

    rpcService.requestVote(RequestVoteRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onError(any(StatusRuntimeException.class));
  }

  @Test
  public void appendEntries_success() {
    StreamObserver<AppendEntriesResponse> responseObserver = mock(StreamObserver.class);
    var response = AppendEntriesResponse.getDefaultInstance();
    when(modeManager.processAppendEntriesRequest(any())).thenReturn(response);

    rpcService.appendEntries(AppendEntriesRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onNext(response);
    verify(responseObserver, times(1)).onCompleted();
  }

  @Test
  public void appendEntries_statusRuntimeExceptionThrown() {
    StreamObserver<AppendEntriesResponse> responseObserver = mock(StreamObserver.class);
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(modeManager.processAppendEntriesRequest(any())).thenThrow(exception);

    rpcService.appendEntries(AppendEntriesRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onError(exception);
  }

  @Test
  public void appendEntries_runtimeExceptionThrown() {
    StreamObserver<AppendEntriesResponse> responseObserver = mock(StreamObserver.class);
    RuntimeException exception = new RuntimeException("test");
    when(modeManager.processAppendEntriesRequest(any())).thenThrow(exception);

    rpcService.appendEntries(AppendEntriesRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onError(any(StatusRuntimeException.class));
  }

  @Test
  public void appendEntries_runtimeExceptionWithoutMessageThrown() {
    StreamObserver<AppendEntriesResponse> responseObserver = mock(StreamObserver.class);
    RuntimeException exception = new RuntimeException();
    when(modeManager.processAppendEntriesRequest(any())).thenThrow(exception);

    rpcService.appendEntries(AppendEntriesRequest.getDefaultInstance(), responseObserver);

    verify(responseObserver, times(1)).onError(any(StatusRuntimeException.class));
  }
}
