package dev.sbutler.bitflask.client.command_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.network.RespReader;
import dev.sbutler.bitflask.resp.network.RespWriter;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RespCommandProcessorTest {

  @InjectMocks
  RespCommandProcessor respCommandProcessor;

  @Mock
  RespReader respReader;
  @Mock
  RespWriter respWriter;

  @Test
  void runCommand() throws ProcessingException, IOException {
    RemoteCommand clientCommand = new RemoteCommand("PING", ImmutableList.of());
    RespBulkString mockResponse = new RespBulkString("ping");
    doReturn(mockResponse).when(respReader).readNextRespElement();
    String result = respCommandProcessor.runCommand(clientCommand);
    assertEquals(mockResponse.toString(), result);
    verify(respWriter, times(1)).writeRespElement(clientCommand.getAsRespArray());
  }

  @Test
  void runCommand_write_IOException() throws IOException {
    RemoteCommand clientCommand = new RemoteCommand("PING", ImmutableList.of());
    doThrow(IOException.class).when(respWriter).writeRespElement(any(RespElement.class));
    assertThrows(ProcessingException.class, () -> respCommandProcessor.runCommand(clientCommand));
    verify(respWriter, times(1)).writeRespElement(clientCommand.getAsRespArray());
    verify(respReader, times(0)).readNextRespElement();
  }

  @Test
  void runCommand_read_IOException() throws IOException {
    RemoteCommand clientCommand = new RemoteCommand("PING", ImmutableList.of());
    doThrow(IOException.class).when(respReader).readNextRespElement();
    assertThrows(ProcessingException.class, () -> respCommandProcessor.runCommand(clientCommand));
    verify(respWriter, times(1)).writeRespElement(clientCommand.getAsRespArray());
    verify(respReader, times(1)).readNextRespElement();
  }

}
