package dev.sbutler.bitflask.storage.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class StorageConfigurationDefaultProviderTest {

  @Test
  void unhandledOptionalName() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider();
    // Act / assert
    assertNull(defaultProvider.getDefaultValueFor("optionName"));
  }

  @Test
  void withoutResourceBundle() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider();
    // Act / assert
    assertEquals(
        String.valueOf(StorageConfigurationDefaultProvider.DEFAULT_STORAGE_DISPATCHER_CAPACITY),
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG));
    assertEquals(
        String.valueOf(StorageConfigurationDefaultProvider.DEFAULT_STORAGE_STORE_DIRECTORY_PATH),
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG));
    assertEquals(
        String.valueOf(StorageConfigurationDefaultProvider.DEFAULT_STORAGE_SEGMENT_SIZE_LIMIT),
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }

  @Test
  void withResourceBundle() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    doReturn(true).when(resourceBundle)
        .containsKey(StorageConfigurationDefaultProvider.STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY);
    String expectedCapacity = "100";
    doReturn(expectedCapacity).when(resourceBundle)
        .getString(StorageConfigurationDefaultProvider.STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY);

    doReturn(true).when(resourceBundle)
        .containsKey(StorageConfigurationDefaultProvider.STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY);
    String expectedPath = "/tmp/store";
    doReturn(expectedPath).when(resourceBundle)
        .getString(StorageConfigurationDefaultProvider.STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY);

    doReturn(true).when(resourceBundle)
        .containsKey(StorageConfigurationDefaultProvider.STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY);
    String expectedLimit = "100";
    doReturn(expectedLimit).when(resourceBundle)
        .getString(StorageConfigurationDefaultProvider.STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY);

    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider(
        resourceBundle);
    // Act / assert
    assertEquals(
        expectedCapacity,
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG));
    assertEquals(
        expectedPath,
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG));
    assertEquals(
        expectedLimit,
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }

  @Test
  void withResourceBundle_keyNotContained() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    doReturn(false).when(resourceBundle)
        .containsKey(StorageConfigurationDefaultProvider.STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY);

    doReturn(false).when(resourceBundle)
        .containsKey(StorageConfigurationDefaultProvider.STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY);
    doReturn(false).when(resourceBundle)
        .containsKey(StorageConfigurationDefaultProvider.STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY);

    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider(
        resourceBundle);
    // Act / assert
    assertEquals(
        String.valueOf(StorageConfigurationDefaultProvider.DEFAULT_STORAGE_DISPATCHER_CAPACITY),
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG));
    assertEquals(
        String.valueOf(StorageConfigurationDefaultProvider.DEFAULT_STORAGE_STORE_DIRECTORY_PATH),
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG));
    assertEquals(
        String.valueOf(StorageConfigurationDefaultProvider.DEFAULT_STORAGE_SEGMENT_SIZE_LIMIT),
        defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }
}
