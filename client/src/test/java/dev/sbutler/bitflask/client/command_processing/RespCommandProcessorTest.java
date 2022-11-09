package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.network.RespService;
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
  RespService respService;

  @Test
  void runCommand() throws ProcessingException, IOException {
    // Arrange
    RemoteCommand clientCommand = new RemoteCommand("PING", ImmutableList.of());
    RespBulkString mockResponse = new RespBulkString("ping");
    doReturn(mockResponse).when(respService).read();
    // Act
    String result = respCommandProcessor.runCommand(clientCommand);
    // Assert
    assertEquals(mockResponse.toString(), result);
    verify(respService, times(1)).write(clientCommand.getAsRespArray());
  }

  @Test
  void runCommand_write_IOException() throws IOException {
    // Arrange
    RemoteCommand clientCommand = new RemoteCommand("PING", ImmutableList.of());
    IOException ioException = new IOException("test");
    doThrow(ioException).when(respService).write(any(RespElement.class));
    // Act
    ProcessingException exception =
        assertThrows(ProcessingException.class,
            () -> respCommandProcessor.runCommand(clientCommand));
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("Failed to write");
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    verify(respService, times(1)).write(clientCommand.getAsRespArray());
    verify(respService, times(0)).read();
  }

  @Test
  void runCommand_read_IOException() throws IOException {
    // Arrange
    RemoteCommand clientCommand = new RemoteCommand("PING", ImmutableList.of());
    IOException ioException = new IOException("test");
    doThrow(ioException).when(respService).read();
    // Act
    ProcessingException exception =
        assertThrows(ProcessingException.class,
            () -> respCommandProcessor.runCommand(clientCommand));
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("Failed to read");
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    verify(respService, times(1)).write(clientCommand.getAsRespArray());
    verify(respService, times(1)).read();
  }
}
