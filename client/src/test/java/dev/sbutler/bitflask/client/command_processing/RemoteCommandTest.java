package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespServiceProvider;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Unit tests for {@link RemoteCommand}. */
public class RemoteCommandTest {

  private static final RespRequest RESP_REQUEST = new RespRequest.PingRequest();

  private final SocketChannel SOCKET_CHANNEL = mock(SocketChannel.class);

  private final OutputWriter outputWriter = mock(OutputWriter.class);
  private final RespCommandProcessor respCommandProcessor = mock(RespCommandProcessor.class);
  private final RespServiceProvider respServiceProvider = mock(RespServiceProvider.class);

  private final RemoteCommand remoteCommand =
      new RemoteCommand(RESP_REQUEST, outputWriter, respCommandProcessor, respServiceProvider);

  @Test
  public void execute_respResponse_success() {
    RespResponse response = new RespResponse.Success("test");
    when(respCommandProcessor.sendRequest(RESP_REQUEST)).thenReturn(response);

    boolean shouldContinue = remoteCommand.execute();

    assertThat(shouldContinue).isTrue();
    verify(outputWriter, times(1)).writeWithNewLine("test");
  }

  @Test
  public void execute_respResponse_failure() {
    RespResponse response = new RespResponse.Failure("test");
    when(respCommandProcessor.sendRequest(RESP_REQUEST)).thenReturn(response);

    boolean shouldContinue = remoteCommand.execute();

    assertThat(shouldContinue).isTrue();
    verify(outputWriter, times(1)).writeWithNewLine("test");
  }

  @Test
  public void execute_respResponse_noKnownLeader() {
    RespResponse response = new RespResponse.NoKnownLeader();
    when(respCommandProcessor.sendRequest(RESP_REQUEST)).thenReturn(response);

    boolean shouldContinue = remoteCommand.execute();

    assertThat(shouldContinue).isFalse();
    verify(outputWriter, times(1)).writeWithNewLine("No leader is currently known.");
  }

  @Test
  public void execute_respResponse_notCurrentLeader_reconnectSuccessful() throws Exception {
    RespResponse response = new RespResponse.NotCurrentLeader("host", 9090);
    when(respCommandProcessor.sendRequest(RESP_REQUEST)).thenReturn(response);
    RespService mockRespService = mock(RespService.class);
    when(respServiceProvider.get()).thenReturn(mockRespService);

    boolean shouldContinue;
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      socketChannelMockedStatic
          .when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(SOCKET_CHANNEL);

      shouldContinue = remoteCommand.execute();

      respServiceMockedStatic.verify(() -> RespService.create(SOCKET_CHANNEL), times(1));
    }

    assertThat(shouldContinue).isTrue();
    verify(mockRespService, times(1)).close();
    verify(respServiceProvider, times(1)).updateRespService(any());
    verify(outputWriter, times(1))
        .writeWithNewLine(
            "Reconnected to the current Bitflask server leader host [host] port [9090]. Retry your command.");
  }

  @Test
  public void execute_respResponse_notCurrentLeader_reconnectFailed() throws Exception {
    RespResponse response = new RespResponse.NotCurrentLeader("host", 9090);
    when(respCommandProcessor.sendRequest(RESP_REQUEST)).thenReturn(response);
    RespService mockRespService = mock(RespService.class);
    when(respServiceProvider.get()).thenReturn(mockRespService);

    boolean shouldContinue;
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      socketChannelMockedStatic
          .when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenThrow(new IOException("test"));

      shouldContinue = remoteCommand.execute();

      respServiceMockedStatic.verify(() -> RespService.create(any()), never());
    }

    assertThat(shouldContinue).isFalse();
    verify(mockRespService, times(1)).close();
    verify(respServiceProvider, never()).updateRespService(any());
    verify(outputWriter, times(1)).writeWithNewLine("Failed to reconnect to new leader. test");
  }
}
