package dev.sbutler.bitflask.client.network;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class SocketChannelProviderTest {

  private ClientConfigurations configs;

  @BeforeEach
  void beforeEach() {
    configs = mock(ClientConfigurations.class);
    when(configs.getHost()).thenReturn("localhost");
    when(configs.getPort()).thenReturn(9090);
  }

  @Test
  void get_success() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class)) {
      // Arrange
      SocketChannelProvider provider = new SocketChannelProvider(configs);

      SocketChannel mockSocketChannel = mock(SocketChannel.class);
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(mockSocketChannel);
      // Act
      SocketChannel socketChannel = provider.get();
      // Assert
      assertThat(socketChannel).isEqualTo(mockSocketChannel);
    }
  }

  @Test
  void get_ioException_throwsProvisionException() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class)) {
      // Arrange
      SocketChannelProvider provider = new SocketChannelProvider(configs);

      IOException ioException = new IOException("test");
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenThrow(ioException);
      // Act
      ProvisionException e = assertThrows(ProvisionException.class, provider::get);
      // Assert
      assertThat(e).hasMessageThat().ignoringCase().contains("failed to provision");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }
}
