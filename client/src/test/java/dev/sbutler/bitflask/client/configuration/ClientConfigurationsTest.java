package dev.sbutler.bitflask.client.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import org.junit.jupiter.api.Test;

public class ClientConfigurationsTest {

  @Test
  void propertyFile() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfigurations configuration = new ClientConfigurations();
    String[] argv = new String[]{};
    // Act
    JCommander.newBuilder()
        .addObject(configuration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertThat(configuration.getHost())
        .isEqualTo(defaultProvider.getDefaultValueFor(
            ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT));
    assertThat(configuration.getHost())
        .isEqualTo(defaultProvider.getDefaultValueFor(
            ClientConfigurationsConstants.SERVER_HOST_FLAG_LONG));
    assertThat(configuration.getPort())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT)));
    assertThat(configuration.getPort())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            ClientConfigurationsConstants.SERVER_PORT_FLAG_LONG)));
    assertThat(configuration.getInlineCmd()).hasSize(0);
    assertThat(configuration.getUsePrompt()).isTrue();
  }

  @Test
  void propertyFile_illegalConfiguration_serverHost() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn(" ").when(defaultProvider)
        .getDefaultValueFor(ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT);
    ClientConfigurations configuration = new ClientConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(configuration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat().contains(
        ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT);
  }

  @Test
  void propertyFile_illegalConfiguration_serverPort() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT);
    ClientConfigurations configuration = new ClientConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(configuration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat().contains(
        ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT);
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfigurations configuration = new ClientConfigurations();
    String[] argv = new String[]{
        ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT,
        "test",
        ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT,
        "9091",
        "get",
        "test"};
    // Act
    JCommander.newBuilder()
        .addObject(configuration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertThat(configuration.getHost()).isEqualTo("test");
    assertThat(configuration.getPort()).isEqualTo(9091);
    assertThat(configuration.getInlineCmd()).containsExactly("get", "test");
    assertThat(configuration.getUsePrompt()).isFalse();
  }

  @Test
  void commandLineFlags_illegalConfiguration_serverHost() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfigurations configuration = new ClientConfigurations();
    String[] argv = new String[]{
        ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT,
        " "};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(configuration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv)
        );
    // Assert
    assertThat(exception).hasMessageThat().contains(
        ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT);
  }

  @Test
  void commandLineFlags_illegalConfiguration_serverPort() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfigurations configuration = new ClientConfigurations();
    String[] argv = new String[]{
        ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT,
        "-1"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(configuration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat().contains(
        ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT);
  }
}
