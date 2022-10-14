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

public class ClientConfigurationTest {

  @Test
  void propertyFile() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfiguration configuration = new ClientConfiguration();
    String[] argv = new String[]{};
    // Act
    JCommander.newBuilder()
        .addObject(configuration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(
        defaultProvider.getDefaultValueFor(ClientConfigurationConstants.SERVER_HOST_FLAG_SHORT),
        configuration.getHost());
    assertEquals(
        defaultProvider.getDefaultValueFor(ClientConfigurationConstants.SERVER_HOST_FLAG_LONG),
        configuration.getHost());
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT)),
        configuration.getPort());
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            ClientConfigurationConstants.SERVER_PORT_FLAG_LONG)),
        configuration.getPort());
  }

  @Test
  void propertyFile_illegalConfiguration_serverHost() {
    // TODO: implement when validator added
  }

  @Test
  void propertyFile_illegalConfiguration_serverPort() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT);
    ClientConfiguration configuration = new ClientConfiguration();
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
        exception.getMessage().contains(ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT));
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfiguration configuration = new ClientConfiguration();
    String[] argv = new String[]{
        ClientConfigurationConstants.SERVER_HOST_FLAG_SHORT,
        "test",
        ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT,
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
    // TODO: implement when validator added
  }

  @Test
  void commandLineFlags_illegalConfiguration_serverPort() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        ClientConfigurationConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    ClientConfiguration configuration = new ClientConfiguration();
    String[] argv = new String[]{
        ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT,
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
        exception.getMessage().contains(ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT));
  }
}
