package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import org.junit.jupiter.api.Test;

public class ServerConfigurationTest {

  @Test
  void propertyFile() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ServerConfigurationConstants.SERVER_FLAG_TO_CONFIGURATION_MAP);
    ServerConfiguration serverConfiguration = new ServerConfiguration();
    String[] argv = new String[]{};
    // Act
    JCommander.newBuilder()
        .addObject(serverConfiguration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            ServerConfigurationConstants.SERVER_PORT_FLAG_SHORT)),
        serverConfiguration.getPort());
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            ServerConfigurationConstants.SERVER_PORT_FLAG_LONG)),
        serverConfiguration.getPort());
  }

  @Test
  void propertyFile_illegalConfiguration_serverPort() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(ServerConfigurationConstants.SERVER_PORT_FLAG_SHORT);

    ServerConfiguration serverConfiguration = new ServerConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(serverConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage().contains(ServerConfigurationConstants.SERVER_PORT_FLAG_SHORT)
    );
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ServerConfigurationConstants.SERVER_FLAG_TO_CONFIGURATION_MAP);
    ServerConfiguration serverConfiguration = new ServerConfiguration();
    String[] argv = new String[]{
        ServerConfigurationConstants.SERVER_PORT_FLAG_SHORT,
        "9091"};
    // Act
    JCommander.newBuilder()
        .addObject(serverConfiguration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(9091, serverConfiguration.getPort());
  }

  @Test
  void commandLineFlags_illegalConfiguration_serverPort() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ServerConfigurationConstants.SERVER_FLAG_TO_CONFIGURATION_MAP);
    ServerConfiguration serverConfiguration = new ServerConfiguration();
    String[] argv = new String[]{
        ServerConfigurationConstants.SERVER_PORT_FLAG_SHORT,
        "-1"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(serverConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage().contains(ServerConfigurationConstants.SERVER_PORT_FLAG_SHORT)
    );
  }
}
