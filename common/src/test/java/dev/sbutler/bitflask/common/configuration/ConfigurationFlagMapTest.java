package dev.sbutler.bitflask.common.configuration;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

public class ConfigurationFlagMapTest {

  @Test
  void builder() {
    // Arrange
    Configuration config0 =
        new Configuration(ImmutableList.of("flag0"), "key0", "value0");
    Configuration config1 =
        new Configuration(ImmutableList.of("flag1_0", "flag1_1"), "key1", "value1");
    ConfigurationFlagMap flagMap =
        new ConfigurationFlagMap.Builder()
            .put(config0.flags().get(0), config0)
            .putAll(config1.flags(), config1)
            .build();
    // Act / Assert
    assertThat(flagMap.get("notFound")).isNull();
    assertThat(flagMap.get("flag0")).isEqualTo(config0);
    assertThat(flagMap.get("flag1_0")).isEqualTo(config1);
    assertThat(flagMap.get("flag1_1")).isEqualTo(config1);
  }
}
