package dev.sbutler.bitflask.common.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableList;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class ConfigurationsBuilderTest {

  private final Configuration config =
      new Configuration(ImmutableList.of("port"), "port", 9090);
  private final ConfigurationFlagMap flagMap =
      new ConfigurationFlagMap.Builder()
          .put("port", config)
          .build();
  private final ResourceBundle resourceBundle = ResourceBundle.getBundle("test");

  @Test
  void build() {
    // Arrange
    String[] args = new String[]{"--port", "8080"};
    TestConfigurations configurations = new TestConfigurations();
    ConfigurationsBuilder builder = new ConfigurationsBuilder(args, resourceBundle);
    // Act
    builder.build(configurations, flagMap);
    // Assert
    assertThat(configurations.getPort()).isEqualTo(8080);
  }

  @Test
  void build_unknownOption() {
    // Arrange
    String[] args = new String[]{"--port", "8080", "--unknown", "invalid"};
    TestConfigurations configurations = new TestConfigurations();
    ConfigurationsBuilder builder = new ConfigurationsBuilder(args, resourceBundle);
    // Act
    ParameterException e =
        assertThrows(ParameterException.class, () -> builder.build(configurations, flagMap));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("--unknown");
  }

  @Test
  void buildAcceptingUnknownOptions() {
    // Arrange
    String[] args = new String[]{"--port", "8080"};
    TestConfigurations configurations = new TestConfigurations();
    ConfigurationsBuilder builder = new ConfigurationsBuilder(args, resourceBundle);
    // Act
    builder.buildAcceptingUnknownOptions(configurations, flagMap);
    // Assert
    assertThat(configurations.getPort()).isEqualTo(8080);
  }

  @Test
  void buildAcceptingUnknownOptions_unknownOption() {
    // Arrange
    String[] args = new String[]{"--port", "8080", "--unknown", "invalid"};
    TestConfigurations configurations = new TestConfigurations();
    ConfigurationsBuilder builder = new ConfigurationsBuilder(args, resourceBundle);
    // Act
    builder.buildAcceptingUnknownOptions(configurations, flagMap);
    // Assert
    assertThat(configurations.getPort()).isEqualTo(8080);
  }

  private static class TestConfigurations implements Configurations {

    @Parameter(names = "--port")
    private int port;

    public int getPort() {
      return port;
    }
  }
}
