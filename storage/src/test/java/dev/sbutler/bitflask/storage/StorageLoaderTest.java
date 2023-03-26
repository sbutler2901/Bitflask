package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.LSMTreeLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class StorageLoaderTest {

  private static final Path DIR_PATH = Path.of("/tmp/.bitflask");

  private final StorageConfigurations config = mock(StorageConfigurations.class);
  private final LSMTreeLoader lsmTreeLoader = mock(LSMTreeLoader.class);

  private final StorageLoader storageLoader = new StorageLoader(config, lsmTreeLoader);

  @BeforeEach
  public void beforeEach() {
    when(config.getStoreDirectoryPath()).thenReturn(DIR_PATH);
  }

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
      assertThat(exception).hasMessageThat()
          .isEqualTo(String.format("Failed to create storage directory path [%s]", DIR_PATH));

      verify(lsmTreeLoader, times(0)).load();
    }
  }
}
