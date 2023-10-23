package dev.sbutler.bitflask.server;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.messages.RespResponseCode;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link ClientMessageProcessor}. */
public class ClientMessageProcessorTest {

  private final ServerCommandFactory serverCommandFactory = mock(ServerCommandFactory.class);
  private final RespService respService = mock(RespService.class);

  private final ClientMessageProcessor clientMessageProcessor =
      new ClientMessageProcessor(serverCommandFactory, respService);

  @BeforeEach
  public void beforeEach() {
    when(respService.isOpen()).thenReturn(true);
  }

  @Test
  public void processNextMessage_success() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    when(serverCommandFactory.createCommand(any())).thenReturn(new ServerCommand.PingCommand());

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    verify(respService, times(1)).write(any());
  }

  @Test
  public void processNextMessage_respService_closed() {
    reset(respService);
    when(respService.isOpen()).thenReturn(false);

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
  }

  @Test
  public void processNextMessage_respService_throwsEOFException() throws Exception {
    when(respService.read()).thenThrow(EOFException.class);

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(respService, times(0)).write(any());
  }

  @Test
  public void processNextMessage_respService_throwsProtocolException() throws Exception {
    when(respService.read()).thenThrow(ProtocolException.class);

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(respService, never()).write(any());
  }

  @Test
  public void processNextMessage_respService_throwsIOException() throws Exception {
    when(respService.read()).thenThrow(IOException.class);

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(respService, never()).write(any());
  }

  @Test
  public void processNextMessage_readMessageNotRespArray_failureResponse() throws Exception {
    when(respService.read()).thenReturn(new RespBulkString("test"));

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    RespResponse respResponse = RespResponse.createFromRespArray(responseCaptor.getValue());
    assertThat(respResponse.getResponseCode()).isEqualTo(RespResponseCode.FAILURE);
    assertThat(respResponse.getMessage()).isEqualTo("Message must be provided in a RespArray");
  }

  @Test
  public void processNextMessage_thrownRespRequestConversionException_failureResponse()
      throws Exception {
    when(respService.read()).thenReturn(new RespArray(ImmutableList.of()));

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    ArgumentCaptor<RespArray> responseCaptor = ArgumentCaptor.forClass(RespArray.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    RespResponse respResponse = RespResponse.createFromRespArray(responseCaptor.getValue());
    assertThat(respResponse.getResponseCode()).isEqualTo(RespResponseCode.FAILURE);
    assertThat(respResponse.getMessage()).isEqualTo("Failed to convert [] to a RespRequest.");
  }

  @Test
  public void processNextMessage_unrecoverableErrorThrown_errorResponse() throws Exception {
    RespElement rawClientMessage = new RespRequest.PingRequest().getAsRespArray();
    when(respService.read()).thenReturn(rawClientMessage);
    RuntimeException exception = new RuntimeException("test");
    when(serverCommandFactory.createCommand(any())).thenThrow(exception);

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    ArgumentCaptor<RespError> responseCaptor = ArgumentCaptor.forClass(RespError.class);
    verify(respService, times(1)).write(responseCaptor.capture());
    assertThat(responseCaptor.getValue().getValue()).isEqualTo("test");
  }

  @Test
  void isOpen() {
    boolean isOpen = clientMessageProcessor.isOpen();

    assertThat(isOpen).isTrue();
    verify(respService, times(1)).isOpen();
  }

  @Test
  void close() throws Exception {
    reset(respService);

    clientMessageProcessor.close();

    verify(respService, times(1)).close();
  }
}
