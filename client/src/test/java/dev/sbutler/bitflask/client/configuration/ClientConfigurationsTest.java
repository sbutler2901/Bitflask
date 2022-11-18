package dev.sbutler.bitflask.client.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    assertEquals(
        defaultProvider.getDefaultValueFor(ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT),
        configuration.getHost());
    assertEquals(
        defaultProvider.getDefaultValueFor(ClientConfigurationsConstants.SERVER_HOST_FLAG_LONG),
        configuration.getHost());
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT)),
        configuration.getPort());
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            ClientConfigurationsConstants.SERVER_PORT_FLAG_LONG)),
        configuration.getPort());
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
    assertTrue(
        exception.getMessage().contains(ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT));
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
    assertTrue(
        exception.getMessage().contains(ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT));
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
        "9091"};
    // Act
    JCommander.newBuilder()
        .addObject(configuration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals("test", configuration.getHost());
    assertEquals(9091, configuration.getPort());
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
    assertTrue(
        exception.getMessage().contains(ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT));
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
    assertTrue(
        exception.getMessage().contains(ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT));
  }
}
