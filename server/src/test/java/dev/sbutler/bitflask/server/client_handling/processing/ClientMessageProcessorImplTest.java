package dev.sbutler.bitflask.server.client_handling.processing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import dev.sbutler.bitflask.server.command_processing.ServerCommand;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientMessageProcessorImplTest {

  @InjectMocks
  ClientMessageProcessorImpl clientMessageProcessor;

  @Mock
  CommandProcessor commandProcessor;
  @Mock
  RespReader respReader;
  @Mock
  RespWriter respWriter;

  @Test
  void processRequest_success() throws IOException {
    RespType<?> clientMessage = new RespArray(List.of(
        new RespBulkString("ping")
    ));
    String responseValue = "pong";
    RespType<?> expectedResponse = new RespBulkString(responseValue);

    doReturn(clientMessage).when(respReader).readNextRespType();
    doReturn(responseValue).when(commandProcessor).processServerCommand(any(ServerCommand.class));

    assertTrue(clientMessageProcessor.processNextMessage());
    verify(respReader, times(1)).readNextRespType();
    verify(commandProcessor, times(1)).processServerCommand(any(ServerCommand.class));
    verify(respWriter, times(1)).writeRespType(expectedResponse);
  }

  @Test
  void readClientMessage_IOException() throws IOException {
    doThrow(IOException.class).when(respReader).readNextRespType();

    assertFalse(clientMessageProcessor.processNextMessage());
    verify(respReader, times(1)).readNextRespType();
    verify(commandProcessor, times(0)).processServerCommand(any(ServerCommand.class));
    verify(respWriter, times(0)).writeRespType(any(RespType.class));
  }

  @Test
  void readClientMessage_EOFException() throws IOException {
    doThrow(EOFException.class).when(respReader).readNextRespType();

    assertFalse(clientMessageProcessor.processNextMessage());
    verify(respReader, times(1)).readNextRespType();
    verify(commandProcessor, times(0)).processServerCommand(any(ServerCommand.class));
    verify(respWriter, times(0)).writeRespType(any(RespType.class));
  }

  @Test
  void getServerResponseToClient_IllegalArgumentException() throws IOException {
    RespType<?> clientMessage = new RespBulkString("ping");

    doReturn(clientMessage).when(respReader).readNextRespType();

    assertTrue(clientMessageProcessor.processNextMessage());
    verify(respReader, times(1)).readNextRespType();
    verify(commandProcessor, times(0)).processServerCommand(any(ServerCommand.class));
    verify(respWriter, times(1)).writeRespType(any(RespType.class));
  }

  @Test
  void writeResponseMessage_IOException() throws IOException {
    RespType<?> clientMessage = new RespArray(List.of(
        new RespBulkString("ping")
    ));
    String responseValue = "pong";
    RespType<?> expectedResponse = new RespBulkString(responseValue);

    doReturn(clientMessage).when(respReader).readNextRespType();
    doReturn(responseValue).when(commandProcessor).processServerCommand(any(ServerCommand.class));
    doThrow(IOException.class).when(respWriter).writeRespType(any(RespType.class));

    assertFalse(clientMessageProcessor.processNextMessage());
    verify(respReader, times(1)).readNextRespType();
    verify(commandProcessor, times(1)).processServerCommand(any(ServerCommand.class));
    verify(respWriter, times(1)).writeRespType(expectedResponse);
  }

}
