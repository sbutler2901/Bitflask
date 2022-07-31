package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class ServerConfigurationTest {

  @Test
  void defaultConfiguration() {
    // Act
    ServerConfiguration serverConfiguration = new ServerConfiguration();
    // Assert
    assertEquals(9090, serverConfiguration.getPort());
  }

  @Test
  void propertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("server.port");
    doReturn("9091").when(resourceBundle).getString("server.port");
    // Act
    ServerConfiguration serverConfiguration = new ServerConfiguration(resourceBundle);
    // Assert
    assertEquals(9091, serverConfiguration.getPort());
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ServerConfiguration serverConfiguration = new ServerConfiguration();
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
    doReturn(true).when(resourceBundle).containsKey("server.port");
    doReturn("9091").when(resourceBundle).getString("server.port");
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
