package dev.sbutler.bitflask.server.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.channels.ServerSocketChannel;
import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  @Test
  void provideServerConfiguration() {
    // Arrange
    ServerConfigurations serverConfigurations = new ServerConfigurations();
    ServerModule serverModule =
        new ServerModule(serverConfigurations, mock(ServerSocketChannel.class));
    // Act
    ServerConfigurations provideServerConfiguration = serverModule.provideServerConfiguration();
    // Assert
    assertThat(provideServerConfiguration).isEqualTo(serverConfigurations);
  }
}
