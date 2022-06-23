package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

// todo: implement tests
@ExtendWith(MockitoExtension.class)
class SegmentLoaderImplTest {

  @InjectMocks
  SegmentLoaderImpl segmentLoader;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentFactory segmentFactory;

  @BeforeEach
  void beforeEach() {
    segmentLoader.logger = mock(Logger.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments() throws IOException, ExecutionException, InterruptedException {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(
        Files.class); MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(
        FileChannel.class)) {
      // Arrange
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      when(pathIterator.next()).thenReturn(firstPath).thenReturn(secondPath);
      doReturn(pathIterator).when(directoryStream).iterator();

      FileTime firstFileTime = mock(FileTime.class);
      FileTime secondFileTime = mock(FileTime.class);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime).thenReturn(secondFileTime);

      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), any(), any()))
          .thenReturn(fileChannel);

      Segment firstSegment = mock(Segment.class);
      Future<Segment> firstSegmentFuture = mock(Future.class);
      doReturn(firstSegment).when(firstSegmentFuture).get();
      Segment secondSegment = mock(Segment.class);
      Future<Segment> secondSegmentFuture = mock(Future.class);
      doReturn(secondSegment).when(secondSegmentFuture).get();
      doReturn(List.of(firstSegmentFuture, secondSegmentFuture)).when(executorService)
          .invokeAll(anyList());

      // Act
      List<Segment> loadedSegments = segmentLoader.loadExistingSegments();

      // Assert
      assertEquals(2, loadedSegments.size());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_noFilesFound() throws IOException {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      doReturn(false).when(pathIterator).hasNext();
      doReturn(pathIterator).when(directoryStream).iterator();
      // Act
      List<Segment> loadedSegments = segmentLoader.loadExistingSegments();
      // Assert
      assertEquals(0, loadedSegments.size());
    }
  }

  @Test
  void loadExistingSegments_DirectoryIteratorException() {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      DirectoryIteratorException directoryIteratorException = mock(
          DirectoryIteratorException.class);
      doReturn(new IOException("directory stream")).when(directoryIteratorException).getCause();
      filesMockedStatic.when(() -> Files.newDirectoryStream(any()))
          .thenThrow(directoryIteratorException);
      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_executorServiceInterruptedException() throws InterruptedException {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(
        Files.class); MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(
        FileChannel.class)) {
      // Arrange
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true).thenReturn(false);
      Path path = mock(Path.class);
      doReturn(path).when(pathIterator).next();
      doReturn(pathIterator).when(directoryStream).iterator();

      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), any(), any()))
          .thenReturn(fileChannel);

      doThrow(InterruptedException.class).when(executorService).invokeAll(anyList());

      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
    }
  }

}
