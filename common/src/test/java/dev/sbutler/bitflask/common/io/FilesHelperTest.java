package dev.sbutler.bitflask.common.io;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FilesHelperTest {

  @InjectMocks
  FilesHelper filesHelper;
  @Mock
  ThreadFactory threadFactory;

  @SuppressWarnings("unchecked")
  @Test
  public void getFilePathsInDirectory() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true, true, false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      when(pathIterator.next()).thenReturn(firstPath, secondPath);
      when(directoryStream.iterator()).thenReturn(pathIterator);
      // Act
      ImmutableList<Path> paths = filesHelper.getFilePathsInDirectory(Path.of("dirPath"));
      // Assert
      assertThat(paths).hasSize(2);
      assertThat(paths).containsExactly(firstPath, secondPath);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getFilePathsInDirectory_directoryIteratorException() {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      IOException ioException = new IOException("test");
      DirectoryIteratorException dirException = new DirectoryIteratorException(ioException);
      when(pathIterator.hasNext()).thenThrow(dirException);
      when(directoryStream.iterator()).thenReturn(pathIterator);
      // Act
      IOException exception =
          assertThrows(IOException.class, () ->
              filesHelper.getFilePathsInDirectory(Path.of("dirPath")));
      // Assert
      assertThat(exception).isEqualTo(ioException);
    }
  }
}
