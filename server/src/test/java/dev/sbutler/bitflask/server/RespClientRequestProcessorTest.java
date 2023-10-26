package dev.sbutler.bitflask.server;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.messages.RespResponseCode;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link RespClientRequestProcessor}. */
public class RespClientRequestProcessorTest {

  private final ServerCommandFactory serverCommandFactory = mock(ServerCommandFactory.class);
  private final RespService respService = mock(RespService.class);

  private final RespClientRequestProcessor respClientRequestProcessor =
      new RespClientRequestProcessor(serverCommandFactory, respService);

  @BeforeEach
  public void beforeEach() {
    when(respService.isOpen()).thenReturn(true);
  }

  @Test
  public void run() throws Exception {
    when(respService.isOpen()).thenReturn(true).thenReturn(false);

    respClientRequestProcessor.run();

    verify(respService, times(1)).close();
  }

  @Test
  public void run_runtimeExceptionThrown() throws Exception {
    when(respService.isOpen()).thenThrow(new RuntimeException("test"));

    respClientRequestProcessor.run();

    verify(respService, times(1)).close();
  }

  @Test
  public void processNextRespRequest_respResponse_success() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    when(serverCommandFactory.createCommand(any())).thenReturn(new ServerCommand.PingCommand());

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertRawResponseWithMessage(responseCaptor.getValue(), RespResponseCode.SUCCESS, "pong");
    verify(respService, never()).close();
  }

  @Test
  public void processNextRespRequest_respResponse_failure() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    ServerCommand serverCommand = mock(ServerCommand.StorageCommand.class);
    when(serverCommandFactory.createCommand(any())).thenReturn(serverCommand);
    when(serverCommand.execute()).thenReturn(new ClientCommandResults.Failure("test"));

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertRawResponseWithMessage(responseCaptor.getValue(), RespResponseCode.FAILURE, "test");
    verify(respService, never()).close();
  }

  @Test
  public void processNextRespRequest_respResponse_notCurrentLeader() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    ServerCommand serverCommand = mock(ServerCommand.StorageCommand.class);
    when(serverCommandFactory.createCommand(any())).thenReturn(serverCommand);
    when(serverCommand.execute())
        .thenReturn(
            new ClientCommandResults.NotCurrentLeader(
                ServerConfig.ServerInfo.newBuilder()
                    .setHost("host")
                    .setRespPort(9090)
                    .buildPartial()));

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    RespResponse respResponse =
        assertRawResponseWithMessage(
            responseCaptor.getValue(),
            RespResponseCode.NOT_CURRENT_LEADER,
            "Current leader: [host=host, respPort=9090].");
    assertThat(((RespResponse.NotCurrentLeader) respResponse).getHost()).isEqualTo("host");
    assertThat(((RespResponse.NotCurrentLeader) respResponse).getRespPort()).isEqualTo(9090);
    verify(respService, never()).close();
  }

  @Test
  public void processNextRespRequest_respResponse_noKnownLeader() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    ServerCommand serverCommand = mock(ServerCommand.StorageCommand.class);
    when(serverCommandFactory.createCommand(any())).thenReturn(serverCommand);
    when(serverCommand.execute()).thenReturn(new ClientCommandResults.NoKnownLeader());

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertRawResponseWithMessage(
        responseCaptor.getValue(),
        RespResponseCode.NO_KNOWN_LEADER,
        "No leader is currently known.");
    verify(respService, never()).close();
  }

  @Test
  public void processNextRespRequest_respService_throwsEOFException() throws Exception {
    when(respService.read()).thenThrow(EOFException.class);

    respClientRequestProcessor.processNextRespRequest();

    verify(respService, times(1)).read();
    verify(respService, times(0)).write(any());
    verify(respService, times(1)).close();
  }

  @Test
  public void processNextRespRequest_respService_throwsProtocolException() throws Exception {
    when(respService.read()).thenThrow(ProtocolException.class);

    respClientRequestProcessor.processNextRespRequest();

    verify(respService, times(1)).read();
    verify(respService, never()).write(any());
    verify(respService, times(1)).close();
  }

  @Test
  public void processNextRespRequest_respService_sendResponse_write_throwsIOException()
      throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    doThrow(new IOException("test")).when(respService).write(any());
    when(serverCommandFactory.createCommand(any())).thenReturn(new ServerCommand.PingCommand());

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertRawResponseWithMessage(responseCaptor.getValue(), RespResponseCode.SUCCESS, "pong");
    verify(respService, times(1)).close();
  }

  @Test
  public void
      processNextRespRequest_respService_sendUnrecoverableErrorToClient_write_throwsIOException()
          throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    doThrow(new IOException("test")).when(respService).write(any());
    RuntimeException exception = new RuntimeException("test");
    when(serverCommandFactory.createCommand(any())).thenThrow(exception);

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespError> responseCaptor = ArgumentCaptor.forClass(RespError.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertThat(responseCaptor.getValue().getValue()).isEqualTo("test");
    verify(respService, times(1)).close();
  }

  @Test
  public void processNextRespRequest_respService_read_throwsIOException() throws Exception {
    when(respService.read()).thenThrow(IOException.class);

    respClientRequestProcessor.processNextRespRequest();

    verify(respService, times(1)).read();
    verify(respService, never()).write(any());
    verify(respService, times(1)).close();
  }

  @Test
  public void processNextRespRequest_readMessageNotRespArray_failureResponse() throws Exception {
    when(respService.read()).thenReturn(new RespBulkString("test"));

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertRawResponseWithMessage(
        responseCaptor.getValue(),
        RespResponseCode.FAILURE,
        "Message must be provided in a RespArray");
    verify(respService, never()).close();
  }

  @Test
  public void processNextRespRequest_thrownRespRequestConversionException_failureResponse()
      throws Exception {
    when(respService.read()).thenReturn(new RespArray(ImmutableList.of()));

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertRawResponseWithMessage(
        responseCaptor.getValue(),
        RespResponseCode.FAILURE,
        "Failed to convert [] to a RespRequest.");
    verify(respService, never()).close();
  }

  @Test
  public void processNextRespRequest_unrecoverableErrorThrown_errorResponse() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    ServerCommand command = mock(ServerCommand.StorageCommand.class);
    when(serverCommandFactory.createCommand(any())).thenReturn(command);
    RuntimeException exception = new RuntimeException("test");
    when(command.execute()).thenThrow(exception);

    respClientRequestProcessor.processNextRespRequest();

    ArgumentCaptor<RespError> responseCaptor = ArgumentCaptor.forClass(RespError.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertThat(responseCaptor.getValue().getValue()).isEqualTo("test");
    verify(respService, times(1)).close();
  }

  @Test
  void triggerShutdown() throws Exception {
    respClientRequestProcessor.triggerShutdown();

    verify(respService, times(1)).close();
  }

  @Test
  void triggerShutdown_respService_throwsIOException() throws Exception {
    doThrow(new IOException("test")).when(respService).close();

    respClientRequestProcessor.triggerShutdown();

    verify(respService, times(1)).close();
  }

  private static RespResponse assertRawResponseWithMessage(
      RespArray rawResponse, RespResponseCode responseCode, String message) {
    RespResponse respResponse = RespResponse.createFromRespArray(rawResponse);
    assertThat(respResponse.getResponseCode()).isEqualTo(responseCode);
    assertThat(respResponse.getMessage()).isEqualTo(message);
    return respResponse;
  }
}
