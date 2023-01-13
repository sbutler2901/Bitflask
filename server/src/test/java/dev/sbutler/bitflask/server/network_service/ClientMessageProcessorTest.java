package dev.sbutler.bitflask.server.network_service;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientMessageProcessorTest {

  ClientMessageProcessor clientMessageProcessor;

  @Mock
  CommandProcessingService commandProcessingService;
  @Mock
  RespService respService;

  @BeforeEach
  void beforeEach() {
    var factory = new ClientMessageProcessor.Factory(commandProcessingService);
    clientMessageProcessor = factory.create(respService);

    when(respService.isOpen()).thenReturn(true);
  }

  @Test
  void processRequest_success() throws Exception {
    // Arrange
    RespElement rawClientMessage = new RespArray(List.of(
        new RespBulkString("ping")
    ));
    String responseValue = "pong";
    RespElement expectedResponse = new RespBulkString(responseValue);
    doReturn(rawClientMessage).when(respService).read();
    doReturn(immediateFuture(responseValue)).when(commandProcessingService)
        .processCommandMessage(any());
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isTrue();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(expectedResponse);
  }

  @Test
  void readClientMessage_EOFException() throws Exception {
    // Arrange
    doThrow(EOFException.class).when(respService).read();
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @Test
  void readClientMessage_ProtocolException() throws Exception {
    // Arrange
    doThrow(ProtocolException.class).when(respService).read();
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @Test
  void readClientMessage_IOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(respService).read();
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  void getServerResponseToClient_InterruptedException() throws Exception {
    // Arrange
    RespElement rawClientMessage = new RespArray(List.of(
        new RespBulkString("ping")
    ));
    doReturn(rawClientMessage).when(respService).read();
    ListenableFuture<String> responseFuture = mock(ListenableFuture.class);
    doThrow(InterruptedException.class).when(responseFuture).get();
    doReturn(responseFuture).when(commandProcessingService).processCommandMessage(any());
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @Test
  void getServerResponseToClient_ExecutionException() throws Exception {
    // Arrange
    RespElement rawClientMessage = new RespArray(List.of(
        new RespBulkString("ping")
    ));
    doReturn(rawClientMessage).when(respService).read();
    doReturn(immediateFailedFuture(new RuntimeException("test")))
        .when(commandProcessingService).processCommandMessage(any());
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @Test
  void writeResponseMessage_IOException() throws Exception {
    // Arrange
    RespElement rawClientMessage = new RespArray(List.of(
        new RespBulkString("ping")
    ));
    String responseValue = "pong";
    doReturn(rawClientMessage).when(respService).read();
    doReturn(immediateFuture(responseValue)).when(commandProcessingService)
        .processCommandMessage(any());
    doThrow(IOException.class).when(respService).write(any());
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(any());
  }

  @Test
  void parseClientMessage_noRespArray() throws Exception {
    // Arrange
    RespElement rawClientMessage = new RespBulkString("ping");
    doReturn(rawClientMessage).when(respService).read();
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @Test
  void parseClientMessage_notRespBulkStringArgs() throws Exception {
    // Arrange
    RespElement rawClientMessage = new RespArray(ImmutableList.of(new RespInteger(1)));
    doReturn(rawClientMessage).when(respService).read();
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any(RespElement.class));
  }

  @Test
  void respServiceClosed() {
    // Arrange
    reset(respService);
    when(respService.isOpen()).thenReturn(false);
    // Act
    boolean processingSuccessful = clientMessageProcessor.processNextMessage();
    // Arrange
    assertThat(processingSuccessful).isFalse();
  }
}
