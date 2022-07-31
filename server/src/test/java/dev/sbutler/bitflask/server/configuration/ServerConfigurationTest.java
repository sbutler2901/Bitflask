package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class ServerConfigurationTest {

  @Test
  void defaultConfiguration() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(false).when(resourceBundle).containsKey(anyString());
    // Act
    ServerConfiguration serverConfiguration = new ServerConfiguration(resourceBundle);
    // Assert
    assertEquals(9090, serverConfiguration.getPort());
  }

  @Test
  void propertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey(anyString());
    doReturn("9091").when(resourceBundle).getString("port");
    // Act
    ServerConfiguration serverConfiguration = new ServerConfiguration(resourceBundle);
    // Assert
    assertEquals(9091, serverConfiguration.getPort());
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(false).when(resourceBundle).containsKey(anyString());
    ServerConfiguration serverConfiguration = new ServerConfiguration(resourceBundle);
    String[] argv = new String[]{"-p", "9091"};
    // Act
    JCommander.newBuilder()
        .addObject(serverConfiguration)
        .build()
        .parse(argv);
    // Assert
    assertEquals(9091, serverConfiguration.getPort());
  }

  @Test
  void commandLineFlags_withPropertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey(anyString());
    doReturn("9091").when(resourceBundle).getString("port");
    ServerConfiguration serverConfiguration = new ServerConfiguration(resourceBundle);
    String[] argv = new String[]{"-p", "9092"};
    // Act
    JCommander.newBuilder()
        .addObject(serverConfiguration)
        .build()
        .parse(argv);
    // Assert
    assertEquals(9092, serverConfiguration.getPort());
  }
}
