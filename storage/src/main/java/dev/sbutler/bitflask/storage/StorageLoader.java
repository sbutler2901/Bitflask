package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;

/**
 * Handles loading all storage state at start up.
 */
final class StorageLoader {

  private final StorageConfigurations configurations;
  private final LSMTree lsmTree;

  @Inject
  StorageLoader(StorageConfigurations configurations, LSMTree lsmTree) {
    this.configurations = configurations;
    this.lsmTree = lsmTree;
  }

  void load() {
    createStorageDirectory();

    lsmTree.load();
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
