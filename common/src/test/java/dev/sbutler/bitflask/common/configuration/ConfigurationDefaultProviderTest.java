package dev.sbutler.bitflask.common.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class ConfigurationDefaultProviderTest {

  private final Configuration firstConfiguration =
      new Configuration(
          ImmutableList.of("flag0"),
          "propertyKey0",
          "defaultValue0");

  private final Configuration secondConfiguration =
      new Configuration(
          ImmutableList.of("flag1_short", "flag1_long"),
          "propertyKey1",
          "defaultValue1");

  private final ConfigurationFlagMap flagToConfigurationMap =
      new ConfigurationFlagMap.Builder()
          .putAll(firstConfiguration.flags(), firstConfiguration)
          .putAll(secondConfiguration.flags(), secondConfiguration)
          .build();

  @Test
  void unhandledOptionalName() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(flagToConfigurationMap);
    // Act / Assert
    assertNull(defaultProvider.getDefaultValueFor("optionName"));
  }

  @Test
  void withoutResourceBundle() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(flagToConfigurationMap);
    // Act / Assert
    assertEquals(
        firstConfiguration.defaultValue(),
        defaultProvider.getDefaultValueFor(firstConfiguration.flags().get(0)));
    assertEquals(
        secondConfiguration.defaultValue(),
        defaultProvider.getDefaultValueFor(secondConfiguration.flags().get(0)));
    assertEquals(
        secondConfiguration.defaultValue(),
        defaultProvider.getDefaultValueFor(secondConfiguration.flags().get(1)));
  }

  @Test
  void withResourceBundle() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle)
        .containsKey(firstConfiguration.propertyKey());
    String expectedValueFirst = "expectedFirst";
    doReturn(expectedValueFirst).when(resourceBundle)
        .getString(firstConfiguration.propertyKey());

    doReturn(true).when(resourceBundle)
        .containsKey(secondConfiguration.propertyKey());
    String expectedValueSecond = "expectedSecond";
    doReturn(expectedValueSecond).when(resourceBundle)
        .getString(secondConfiguration.propertyKey());

    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(flagToConfigurationMap, resourceBundle);
    // Act / Assert
    assertEquals(expectedValueFirst,
        defaultProvider.getDefaultValueFor(firstConfiguration.flags().get(0)));
    assertEquals(expectedValueSecond,
        defaultProvider.getDefaultValueFor(secondConfiguration.flags().get(0)));
    assertEquals(expectedValueSecond,
        defaultProvider.getDefaultValueFor(secondConfiguration.flags().get(1)));
  }

  @Test
  void withResourceBundle_keyNotContained() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(false).when(resourceBundle)
        .containsKey(firstConfiguration.propertyKey());

    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        flagToConfigurationMap,
        resourceBundle);
    // Act / Assert
    assertEquals(
        firstConfiguration.defaultValue(),
        defaultProvider.getDefaultValueFor(firstConfiguration.flags().get(0)));
  }
}
