package dev.sbutler.bitflask.storage.raft;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

/** Tests for {@link RaftRpcService}. */
public class RaftRpcServiceTest {

  private RaftModeManager modeManager;

  private RaftRpcService rpcService;

  @BeforeEach
  public void beforeEach() {
    modeManager = mock(RaftModeManager.class);
    rpcService = new RaftRpcService(modeManager);
  }

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
    //    doThrow(exception).when(modeManager).processRequestVoteRequest(any());
    when(modeManager.processRequestVoteRequest(any())).thenThrow(exception);

    StatusRuntimeException thrown =
        assertThrows(
            StatusRuntimeException.class,
            () ->
                rpcService.requestVote(RequestVoteRequest.getDefaultInstance(), responseObserver));

    verify(responseObserver, times(1)).onError(exception);
  }

  @Test
  public void requestVote_exceptionThrown() {
    StreamObserver<RequestVoteResponse> responseObserver = mock(StreamObserver.class);
    Exception exception = new Exception();
    when(modeManager.processRequestVoteRequest(any())).thenThrow(exception);

    Exception thrown =
        assertThrows(
            Exception.class,
            () ->
                rpcService.requestVote(RequestVoteRequest.getDefaultInstance(), responseObserver));

    verify(responseObserver, times(1)).onError(exception);
  }
}
