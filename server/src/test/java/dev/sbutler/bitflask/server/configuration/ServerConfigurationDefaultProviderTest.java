package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class ServerConfigurationDefaultProviderTest {

  @Test
  void unhandledOptionalName() {
    // Arrange
    ServerConfigurationDefaultProvider defaultProvider = new ServerConfigurationDefaultProvider();
    // Act / Assert
    assertNull(defaultProvider.getDefaultValueFor("optionName"));
  }

  @Test
  void withoutResourceBundle() {
    // Arrange
    ServerConfigurationDefaultProvider defaultProvider = new ServerConfigurationDefaultProvider();
    // Act / Assert
    assertEquals(
        String.valueOf(ServerConfigurationDefaultProvider.DEFAULT_SERVER_PORT),
        defaultProvider.getDefaultValueFor(
            ServerConfiguration.SERVER_PORT_FLAG_SHORT));
    assertEquals(
        String.valueOf(ServerConfigurationDefaultProvider.DEFAULT_SERVER_PORT),
        defaultProvider.getDefaultValueFor(
            ServerConfiguration.SERVER_PORT_FLAG_LONG));
  }

  @Test
  void withResourceBundle() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    doReturn(true).when(resourceBundle)
        .containsKey(ServerConfigurationDefaultProvider.SERVER_PORT_PROPERTY_KEY);
    String expectedPort = "9091";
    doReturn(expectedPort).when(resourceBundle)
        .getString(ServerConfigurationDefaultProvider.SERVER_PORT_PROPERTY_KEY);

    ServerConfigurationDefaultProvider defaultProvider = new ServerConfigurationDefaultProvider(
        resourceBundle);
    // Act / Assert
    assertEquals(expectedPort,
        defaultProvider.getDefaultValueFor(ServerConfiguration.SERVER_PORT_FLAG_SHORT));
    assertEquals(expectedPort,
        defaultProvider.getDefaultValueFor(ServerConfiguration.SERVER_PORT_FLAG_LONG));
  }

  @Test
  void withResourceBundle_keyNotContained() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(false).when(resourceBundle)
        .containsKey(ServerConfigurationDefaultProvider.SERVER_PORT_PROPERTY_KEY);

    ServerConfigurationDefaultProvider defaultProvider = new ServerConfigurationDefaultProvider(
        resourceBundle);
    // Act / Assert
    assertEquals(
        String.valueOf(ServerConfigurationDefaultProvider.DEFAULT_SERVER_PORT),
        defaultProvider.getDefaultValueFor(
            ServerConfiguration.SERVER_PORT_FLAG_SHORT));
    assertEquals(
        String.valueOf(ServerConfigurationDefaultProvider.DEFAULT_SERVER_PORT),
        defaultProvider.getDefaultValueFor(
            ServerConfiguration.SERVER_PORT_FLAG_LONG));
  }
}
