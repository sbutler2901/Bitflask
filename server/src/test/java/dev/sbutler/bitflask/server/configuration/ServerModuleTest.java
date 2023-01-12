package dev.sbutler.bitflask.server.configuration;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  @Test
  void provideServerConfiguration() {
    // Arrange
    ServerConfigurations serverConfigurations = new ServerConfigurations();
    ServerModule serverModule = new ServerModule(serverConfigurations);
    // Act
    ServerConfigurations provideServerConfiguration = serverModule.provideServerConfiguration();
    // Assert
    assertThat(provideServerConfiguration).isEqualTo(serverConfigurations);
  }
}
