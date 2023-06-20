package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.LSMTreeLoader;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

/**
 * Handles loading all storage state at start up.
 */
final class StorageLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageConfigurations configurations;
  private final LSMTreeLoader lsmTreeLoader;

  @Inject
  StorageLoader(StorageConfigurations configurations, LSMTreeLoader lsmTreeLoader) {
    this.configurations = configurations;
    this.lsmTreeLoader = lsmTreeLoader;
  }

  public void load() {
    Instant startInstant = Instant.now();
    createStorageDirectory();
    lsmTreeLoader.load();
    logger.atInfo().log("Loaded Storage in [%d]ms",
        Duration.between(startInstant, Instant.now()).toMillis());
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
