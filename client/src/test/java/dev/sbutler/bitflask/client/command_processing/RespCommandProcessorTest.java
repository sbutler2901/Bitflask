package dev.sbutler.bitflask.client.command_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
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
    ClientCommand clientCommand = new ClientCommand("PING", null);
    RespBulkString mockResponse = new RespBulkString("ping");
    doReturn(mockResponse).when(respReader).readNextRespType();
    String result = respCommandProcessor.runCommand(clientCommand);
    assertEquals(mockResponse.toString(), result);
    verify(respWriter, times(1)).writeRespType(clientCommand.getAsRespArray());
  }

}
