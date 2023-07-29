package dev.sbutler.bitflask.server.network_service;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import dev.sbutler.bitflask.server.command_processing_service.InvalidCommandException;
import dev.sbutler.bitflask.server.command_processing_service.StorageProcessingException;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ClientMessageProcessor}. */
public class ClientMessageProcessorTest {

  private ClientMessageProcessor clientMessageProcessor;

  private final CommandProcessingService commandProcessingService =
      mock(CommandProcessingService.class);
  private final RespService respService = mock(RespService.class);

  @BeforeEach
  void beforeEach() {
    var factory = new ClientMessageProcessor.Factory(commandProcessingService);
    clientMessageProcessor = factory.create(respService);

    when(respService.isOpen()).thenReturn(true);
  }

  @Test
  void processNextMessage_success() throws Exception {
    RespElement rawClientMessage = new RespArray(List.of(new RespBulkString("ping")));
    doReturn(rawClientMessage).when(respService).read();
    String responseValue = "pong";
    doReturn(responseValue).when(commandProcessingService).processCommandMessage(any());

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespBulkString.class));
  }

  @Test
  void processNextMessage_commandProcessing_throwsInvalidCommandException() throws Exception {
    RespElement rawClientMessage = new RespArray(List.of(new RespBulkString("invalidCommand")));
    doReturn(rawClientMessage).when(respService).read();
    InvalidCommandException exception =
        new InvalidCommandException(String.format("Invalid command: [%s]", "invalidCommand"));
    doThrow(exception).when(commandProcessingService).processCommandMessage(any());

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespError.class));
  }

  @Test
  void processNextMessage_commandProcessing_throwsStorageProcessingException() throws Exception {
    RespElement rawClientMessage = new RespArray(List.of(new RespBulkString("invalidCommand")));
    doReturn(rawClientMessage).when(respService).read();
    StorageProcessingException exception =
        new StorageProcessingException(
            String.format("Unexpected failure getting [%s]", "invalidCommand"));
    doThrow(exception).when(commandProcessingService).processCommandMessage(any());

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespError.class));
  }

  @Test
  void processNextMessage_commandProcessing_throwsUnexpectedException() throws Exception {
    RespElement rawClientMessage = new RespArray(List.of(new RespBulkString("invalidCommand")));
    doReturn(rawClientMessage).when(respService).read();
    RuntimeException exception = new RuntimeException("test");
    doThrow(exception).when(commandProcessingService).processCommandMessage(any());

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespError.class));
  }

  @Test
  void readClientMessage_EOFException() throws Exception {
    doThrow(EOFException.class).when(respService).read();

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any());
  }

  @Test
  void readClientMessage_ProtocolException() throws Exception {
    doThrow(ProtocolException.class).when(respService).read();

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any());
  }

  @Test
  void readClientMessage_IOException() throws Exception {
    doThrow(IOException.class).when(respService).read();

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(0)).write(any());
  }

  @Test
  void writeResponseMessage_IOException() throws Exception {
    RespElement rawClientMessage = new RespArray(List.of(new RespBulkString("ping")));
    doReturn(rawClientMessage).when(respService).read();
    String responseValue = "pong";
    doReturn(responseValue).when(commandProcessingService).processCommandMessage(any());
    doThrow(IOException.class).when(respService).write(any());

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(1)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespBulkString.class));
  }

  @Test
  void parseClientMessage_noRespArray() throws Exception {
    RespElement rawClientMessage = new RespBulkString("ping");
    doReturn(rawClientMessage).when(respService).read();

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespError.class));
  }

  @Test
  void parseClientMessage_notRespBulkStringArgs() throws Exception {
    RespElement rawClientMessage = new RespArray(ImmutableList.of(new RespInteger(1)));
    doReturn(rawClientMessage).when(respService).read();

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isTrue();
    verify(respService, times(1)).read();
    verify(commandProcessingService, times(0)).processCommandMessage(any());
    verify(respService, times(1)).write(any(RespError.class));
  }

  @Test
  void respServiceClosed() {
    reset(respService);
    when(respService.isOpen()).thenReturn(false);

    boolean processingSuccessful = clientMessageProcessor.processNextMessage();

    assertThat(processingSuccessful).isFalse();
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
