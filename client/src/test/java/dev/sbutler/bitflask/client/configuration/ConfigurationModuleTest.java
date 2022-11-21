package dev.sbutler.bitflask.client.configuration;

import static com.google.common.truth.Truth.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.configuration.ConfigurationFlagMap;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class ConfigurationModuleTest {

  private static final String[] args = new String[]{"-h", "localhost", "-p", "9090"};

  @Test
  void configure() {
    // Arrange
    Injector injector = Guice.createInjector(new ConfigurationModule(args));
    // Act / Assert
    injector.getBinding(ConfigurationFlagMap.class);
    injector.getBinding(ResourceBundle.class);
    injector.getBinding(ClientConfigurations.class);
    injector.getBinding(ConfigurationsBuilder.class);
  }

  @Test
  void provideClientConfiguration() {
    // Arrange
    Injector injector = Guice.createInjector(new ConfigurationModule(args));
    // Act
    ClientConfigurations configs = injector.getInstance(ClientConfigurations.class);
    // Assert
    assertThat(configs.getHost()).isEqualTo("localhost");
    assertThat(configs.getPort()).isEqualTo(9090);
  }
}
