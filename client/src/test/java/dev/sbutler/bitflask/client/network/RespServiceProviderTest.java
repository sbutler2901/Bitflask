package dev.sbutler.bitflask.client.network;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class RespServiceProviderTest {

  @Test
  void get_success() throws Exception {
    try (MockedConstruction<RespService.Factory> factoryMockedConstruction = mockConstruction(
        RespService.Factory.class)) {
      // Arrange
      RespServiceProvider provider = new RespServiceProvider(mock(SocketChannel.class));
      RespService.Factory factory = factoryMockedConstruction.constructed().get(0);
      RespService mockRespService = mock(RespService.class);
      when(factory.create()).thenReturn(mockRespService);
      // Act
      RespService respService = provider.get();
      // Assert
      assertThat(respService).isEqualTo(mockRespService);
    }
  }

  @Test
  void get_ioException_throwsProvisionException() throws Exception {
    try (MockedConstruction<RespService.Factory> factoryMockedConstruction = mockConstruction(
        RespService.Factory.class)) {
      // Arrange
      RespServiceProvider provider = new RespServiceProvider(mock(SocketChannel.class));
      RespService.Factory factory = factoryMockedConstruction.constructed().get(0);
      IOException ioException = new IOException("test");
      when(factory.create()).thenThrow(ioException);
      // Act
      ProvisionException e = assertThrows(ProvisionException.class, provider::get);
      // Assert
      assertThat(e).hasMessageThat().ignoringCase().contains("failed to provision");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }
}
