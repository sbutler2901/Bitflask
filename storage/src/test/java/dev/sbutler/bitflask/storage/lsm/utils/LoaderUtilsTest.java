package dev.sbutler.bitflask.storage.lsm.utils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings("unchecked")
public class LoaderUtilsTest {

  @Test
  public void loadPathsInDirForGlob_success() {
    Path segPath = Path.of("/tmp/test.seg");
    DirectoryStream<Path> dirStream = mock(DirectoryStream.class);
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.exists(any())).thenReturn(true);
      filesMockedStatic
          .when(() -> Files.newDirectoryStream(any(), anyString()))
          .thenReturn(dirStream);
      when(dirStream.iterator()).thenReturn(ImmutableList.of(segPath).iterator());

      ImmutableList<Path> matchedPaths =
          LoaderUtils.loadPathsInDirForGlob(Path.of("/tmp"), "*.seg");
      assertThat(matchedPaths).containsExactly(segPath);
    }
  }

  @Test
  public void loadPathsInDirForGlob_dirDoesNotExist_throwsStorageLoadException() {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.exists(any())).thenReturn(false);

      StorageLoadException e =
          assertThrows(
              StorageLoadException.class,
              () -> LoaderUtils.loadPathsInDirForGlob(Path.of("/tmp"), "*.seg"));

      assertThat(e).hasMessageThat().isEqualTo("Directory does not exist [/tmp]");
    }
  }

  @Test
  public void loadPathsInDirForGlob_ioException_throwsStorageLoadException() {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.exists(any())).thenReturn(true);
      IOException ioException = new IOException("test");
      filesMockedStatic
          .when(() -> Files.newDirectoryStream(any(), anyString()))
          .thenThrow(ioException);

      StorageLoadException e =
          assertThrows(
              StorageLoadException.class,
              () -> LoaderUtils.loadPathsInDirForGlob(Path.of("/tmp"), "*.seg"));

      assertThat(e).hasCauseThat().isEqualTo(ioException);
      assertThat(e)
          .hasMessageThat()
          .isEqualTo("Failed to load paths in storage dir [/tmp] for glob [*.seg]");
    }
  }

  @Test
  public void deletePathsInDirForGlob() {
    Path segPath = Path.of("/tmp/test.seg");
    DirectoryStream<Path> dirStream = mock(DirectoryStream.class);
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.exists(any())).thenReturn(true);
      filesMockedStatic
          .when(() -> Files.newDirectoryStream(any(), anyString()))
          .thenReturn(dirStream);
      when(dirStream.iterator()).thenReturn(ImmutableList.of(segPath).iterator());

      LoaderUtils.deletePathsInDirForGlob(Path.of("/tmp"), "*.seg");

      filesMockedStatic.verify(() -> Files.delete(eq(segPath)), times(1));
    }
  }

  @Test
  public void deletePathsInDirForGlob_ioException_throwsStorageLoadException() {
    Path segPath = Path.of("/tmp/test.seg");
    DirectoryStream<Path> dirStream = mock(DirectoryStream.class);
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.exists(any())).thenReturn(true);
      filesMockedStatic
          .when(() -> Files.newDirectoryStream(any(), anyString()))
          .thenReturn(dirStream);
      when(dirStream.iterator()).thenReturn(ImmutableList.of(segPath).iterator());

      IOException ioException = new IOException("test");
      filesMockedStatic.when(() -> Files.delete(any())).thenThrow(ioException);

      StorageLoadException e =
          assertThrows(
              StorageLoadException.class,
              () -> LoaderUtils.deletePathsInDirForGlob(Path.of("/tmp"), "*.seg"));

      assertThat(e).hasCauseThat().isEqualTo(ioException);
      assertThat(e).hasMessageThat().isEqualTo(String.format("Failed to delete [%s]", segPath));
    }
  }
}
