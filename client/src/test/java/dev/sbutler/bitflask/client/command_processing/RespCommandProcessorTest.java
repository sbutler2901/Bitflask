package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RespCommandProcessor}. */
public class RespCommandProcessorTest {

  private final RespService respService = mock(RespService.class);

  private final RespCommandProcessor respCommandProcessor =
      new RespCommandProcessor(() -> respService);

  @Test
  public void sendRequest() throws Exception {
    RespRequest request = new RespRequest.PingRequest();
    RespResponse expectedResponse = new RespResponse.Success("pong");
    when(respService.read()).thenReturn(expectedResponse.getAsRespArray());

    RespResponse response = respCommandProcessor.sendRequest(request);

    assertThat(response).isEqualTo(expectedResponse);
    verify(respService, times(1)).write(request.getAsRespArray());
  }

  @Test
  public void sendRequest_write_throwsIOException_throwProcessingException() throws Exception {
    RespRequest request = new RespRequest.PingRequest();
    IOException ioException = new IOException("test");
    doThrow(ioException).when(respService).write(any(RespElement.class));

    ProcessingException exception =
        assertThrows(ProcessingException.class, () -> respCommandProcessor.sendRequest(request));

    assertThat(exception).hasMessageThat().contains("Failed to write");
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    verify(respService, times(1)).write(request.getAsRespArray());
    verify(respService, times(0)).read();
  }

  @Test
  public void sendRequest_read_returnsRespError_throwProcessingException() throws Exception {
    RespRequest request = new RespRequest.PingRequest();
    RespError readError = new RespError("test");
    when(respService.read()).thenReturn(readError);

    ProcessingException exception =
        assertThrows(ProcessingException.class, () -> respCommandProcessor.sendRequest(request));

    assertThat(exception)
        .hasMessageThat()
        .contains("Server responded with unrecoverable error. test");
    verify(respService, times(1)).write(request.getAsRespArray());
    verify(respService, times(1)).read();
  }

  @Test
  public void sendRequest_read_doesNotReturnRespArray_throwProcessingException() throws Exception {
    RespRequest request = new RespRequest.PingRequest();
    RespBulkString readString = new RespBulkString("test");
    when(respService.read()).thenReturn(readString);

    ProcessingException exception =
        assertThrows(ProcessingException.class, () -> respCommandProcessor.sendRequest(request));

    assertThat(exception)
        .hasMessageThat()
        .contains(
            String.format(
                "The server did not return an expected RespElement. Received [%s]",
                readString.getClass()));
    verify(respService, times(1)).write(request.getAsRespArray());
    verify(respService, times(1)).read();
  }

  @Test
  public void sendRequest_read_throwsIOException_throwProcessingException() throws Exception {
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
