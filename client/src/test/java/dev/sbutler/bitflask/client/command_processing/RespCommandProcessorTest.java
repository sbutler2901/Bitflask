package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespElement;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RespCommandProcessor}. */
public class RespCommandProcessorTest {

  private final RespService respService = mock(RespService.class);

  private final RespCommandProcessor respCommandProcessor =
      new RespCommandProcessor(() -> respService);

  @Test
  void runCommand() throws ProcessingException, IOException {
    RespRequest request = new RespRequest.PingRequest();
    RespResponse expectedResponse = new RespResponse.Success("pong");
    doReturn(expectedResponse.getAsRespArray()).when(respService).read();

    RespResponse response = respCommandProcessor.sendRequest(request);

    assertThat(response).isEqualTo(expectedResponse);
    verify(respService, times(1)).write(request.getAsRespArray());
  }

  @Test
  void runCommand_write_IOException() throws IOException {
    RespRequest request = new RespRequest.PingRequest();
    IOException ioException = new IOException("test");
    doThrow(ioException).when(respService).write(any(RespElement.class));

    ProcessingException exception =
        assertThrows(ProcessingException.class, () -> respCommandProcessor.sendRequest(request));

    assertThat(exception).hasMessageThat().ignoringCase().contains("Failed to write");
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    verify(respService, times(1)).write(request.getAsRespArray());
    verify(respService, times(0)).read();
  }

  @Test
  void runCommand_read_IOException() throws IOException {
    RespRequest request = new RespRequest.PingRequest();
    IOException ioException = new IOException("test");
    doThrow(ioException).when(respService).read();

    ProcessingException exception =
        assertThrows(ProcessingException.class, () -> respCommandProcessor.sendRequest(request));

    assertThat(exception).hasMessageThat().ignoringCase().contains("Failed to read");
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    verify(respService, times(1)).write(request.getAsRespArray());
    verify(respService, times(1)).read();
  }
}
