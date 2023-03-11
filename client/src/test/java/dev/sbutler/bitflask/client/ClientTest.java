package dev.sbutler.bitflask.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ClientTest {

  private final Injector INJECTOR = mock(Injector.class);
  private final ReplClientProcessorService REPL_PROCESSOR =
      mock(ReplClientProcessorService.class);
  private final RespService RESP_SERVICE =
      mock(RespService.class);
  private final SocketChannel SOCKET_CHANNEL = mock(SocketChannel.class);

  @Test
  void success() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(SOCKET_CHANNEL);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(RESP_SERVICE);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(INJECTOR);
      when(INJECTOR.getInstance(ReplClientProcessorService.class))
          .thenReturn(REPL_PROCESSOR);
      // Act
      Client.main(new String[0]);
      // Assert
      verify(REPL_PROCESSOR, times(1)).run();
    }
  }

  @Test
  void getInstance_throwsProvisionException() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(SOCKET_CHANNEL);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(RESP_SERVICE);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(INJECTOR);
      when(INJECTOR.getInstance(ReplClientProcessorService.class))
          .thenThrow(new ProvisionException("test"));
      // Act
      Client.main(new String[0]);
      // Assert
      verify(REPL_PROCESSOR, times(0)).run();
    }
  }

  @Test
  void getInstance_throwsConfigurationException() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(SOCKET_CHANNEL);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(RESP_SERVICE);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(INJECTOR);
      when(INJECTOR.getInstance(ReplClientProcessorService.class))
          .thenThrow(new ConfigurationException(ImmutableList.of()));

      // Act
      Client.main(new String[0]);
      // Assert
      verify(REPL_PROCESSOR, times(0)).run();
    }
  }

  @Test
  void unexpectedFailure() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(SOCKET_CHANNEL);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(RESP_SERVICE);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(INJECTOR);
      when(INJECTOR.getInstance(ReplClientProcessorService.class))
          .thenReturn(REPL_PROCESSOR);

      doThrow(new RuntimeException("test")).when(REPL_PROCESSOR).run();

      // Act
      Client.main(new String[0]);
      // Assert
      verify(REPL_PROCESSOR, times(1)).run();
    }
  }

  @Test
  void closeConnection_throwsIoException() throws Exception {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(SOCKET_CHANNEL);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(RESP_SERVICE);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(INJECTOR);
      when(INJECTOR.getInstance(ReplClientProcessorService.class))
          .thenReturn(REPL_PROCESSOR);

      doThrow(new IOException("test")).when(RESP_SERVICE).close();

      // Act
      Client.main(new String[0]);
      // Assert
      verify(REPL_PROCESSOR, times(1)).run();
    }
  }
}
