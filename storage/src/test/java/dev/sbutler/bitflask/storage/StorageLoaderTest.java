package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.LSMTreeLoader;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Unit tests for {@link StorageLoader}. */
public class StorageLoaderTest {

  private static final StorageConfig STORAGE_CONFIG =
      StorageConfig.newBuilder().setStoreDirectoryPath("/tmp/.bitflask").buildPartial();

  private final LSMTreeLoader lsmTreeLoader = mock(LSMTreeLoader.class);

  private final StorageLoader storageLoader = new StorageLoader(STORAGE_CONFIG, lsmTreeLoader);

  @Test
  public void load() {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      storageLoader.load();

      filesMockedStatic.verify(() -> Files.createDirectories(any()), times(1));

      verify(lsmTreeLoader, times(1)).load();
    }
  }

  @Test
  public void load_createStorageDirectoryThrowsIoException_throwStorageLoadException() {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      IOException ioException = new IOException("test");
      filesMockedStatic.when(() -> Files.createDirectories(any())).thenThrow(ioException);

      StorageLoadException exception =
          assertThrows(StorageLoadException.class, storageLoader::load);

      assertThat(exception).hasCauseThat().isEqualTo(ioException);
      assertThat(exception)
          .hasMessageThat()
          .isEqualTo(
              String.format(
                  "Failed to create storage directory path [%s]",
                  STORAGE_CONFIG.getStoreDirectoryPath()));

      verify(lsmTreeLoader, times(0)).load();
    }
  }
}
