package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.LSMTreeLoader;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;

/**
 * Handles loading all storage state at start up.
 */
final class StorageLoader {

  private final StorageConfigurations configurations;
  private final LSMTreeLoader lsmTreeLoader;

  @Inject
  StorageLoader(StorageConfigurations configurations, LSMTreeLoader lsmTreeLoader) {
    this.configurations = configurations;
    this.lsmTreeLoader = lsmTreeLoader;
  }

  public void load() {
    createStorageDirectory();
    lsmTreeLoader.load();
  }

  /**
   * Creates the storage directory and any parents, if non-existent.
   */
  private void createStorageDirectory() {
    try {
      Files.createDirectories(configurations.getStoreDirectoryPath());
    } catch (IOException e) {
      throw new StorageLoadException(
          String.format(
              "Failed to create storage directory path [%s]",
              configurations.getStoreDirectoryPath()),
          e);
    }
  }
}
