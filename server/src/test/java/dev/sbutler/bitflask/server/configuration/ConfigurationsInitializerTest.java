package dev.sbutler.bitflask.server.configuration;

import static com.google.common.truth.Truth.assertThat;

import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.server.configuration.ConfigurationsInitializer.InitializedConfigurations;
import java.util.ResourceBundle;
import org.junit.Test;

public class ConfigurationsInitializerTest {

  @Test
  public void initializeConfigurations() {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("testConfig");
    String[] args = new String[]{};
    ConfigurationsBuilder configurationsBuilder = new ConfigurationsBuilder(args, resourceBundle);
    ConfigurationsInitializer configurationsInitializer = new ConfigurationsInitializer(
        configurationsBuilder);

    InitializedConfigurations initializedConfigurations = configurationsInitializer.initializeConfigurations();

    assertThat(initializedConfigurations.serverConfigurations()).isNotNull();
    assertThat(initializedConfigurations.storageConfigurations()).isNotNull();
  }
}
