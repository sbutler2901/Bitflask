package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentLoaderImplTest {

  @InjectMocks
  SegmentLoaderImpl segmentLoader;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentFileFactory segmentFileFactory;
  @Mock
  SegmentFactory segmentFactory;

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments() throws Exception {
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

      FileTime firstFileTime = FileTime.from(0L, TimeUnit.SECONDS);
      FileTime secondFileTime = FileTime.from(10L, TimeUnit.SECONDS);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime).thenReturn(secondFileTime);

      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), anySet()))
          .thenReturn(fileChannel);
      InOrder fileChannelOrder = inOrder(FileChannel.class);

      SegmentFile segmentFile = mock(SegmentFile.class);
      doReturn(segmentFile).when(segmentFileFactory).create(any(), any(), anyInt());

      Segment firstSegment = mock(Segment.class);
      Future<Segment> firstSegmentFuture = mock(Future.class);
      doReturn(firstSegment).when(firstSegmentFuture).get();
      Segment secondSegment = mock(Segment.class);
      Future<Segment> secondSegmentFuture = mock(Future.class);
      doReturn(secondSegment).when(secondSegmentFuture).get();
      doReturn(List.of(firstSegmentFuture, secondSegmentFuture)).when(executorService)
          .invokeAll(anyList());

      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();

      // Assert
      assertEquals(firstSegment, managedSegments.getWritableSegment());
      assertEquals(1, managedSegments.getFrozenSegments().size());
      assertEquals(secondSegment, managedSegments.getFrozenSegments().get(0));
      // Verify path sorted order maintained
      fileChannelOrder.verify(fileChannelMockedStatic,
          () -> FileChannel.open(eq(secondPath), anySet()));
      fileChannelOrder.verify(fileChannelMockedStatic,
          () -> FileChannel.open(eq(firstPath), anySet()));
    }
  }

  @Test
  void loadExistingSegments_directoryCreated() throws Exception {
    // Arrange
    doReturn(true).when(segmentFactory).createSegmentStoreDir();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(segmentFactory).createSegment();
    // Act
    ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
    // Assert
    assertEquals(writableSegment, managedSegments.getWritableSegment());
    assertEquals(0, managedSegments.getFrozenSegments().size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_noFilesFound() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      doReturn(false).when(segmentFactory).createSegmentStoreDir();
      Segment writableSegment = mock(Segment.class);
      doReturn(writableSegment).when(segmentFactory).createSegment();
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      doReturn(false).when(pathIterator).hasNext();
      doReturn(pathIterator).when(directoryStream).iterator();
      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
      // Assert
      assertEquals(writableSegment, managedSegments.getWritableSegment());
      assertEquals(0, managedSegments.getFrozenSegments().size());
    }
  }

  @Test
  void loadExistingSegments_filePathsDirectoryIteratorException() {
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
  void loadExistingSegments_fileChannelsIOException() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(
        Files.class); MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(
        FileChannel.class)) {
      // Arrange
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true)
          .thenReturn(false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      Path thirdPath = mock(Path.class);
      when(pathIterator.next()).thenReturn(firstPath).thenReturn(secondPath).thenReturn(thirdPath);
      doReturn(pathIterator).when(directoryStream).iterator();

      FileTime firstFileTime = mock(FileTime.class);
      FileTime secondFileTime = mock(FileTime.class);
      FileTime thirdFileTime = mock(FileTime.class);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime).thenReturn(secondFileTime).thenReturn(thirdFileTime);

      FileChannel firstFileChannel = mock(FileChannel.class);
      FileChannel secondFileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), anySet()))
          .thenReturn(firstFileChannel).thenReturn(secondFileChannel).thenThrow(IOException.class);
      doThrow(IOException.class).when(secondFileChannel).close();

      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
      verify(firstFileChannel, times(1)).close();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_executorServiceInterruptedException() throws Exception {
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

      FileTime firstFileTime = mock(FileTime.class);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime);

      SegmentFile segmentFile = mock(SegmentFile.class);
      doReturn(segmentFile).when(segmentFileFactory).create(any(), any(), anyInt());

      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), anySet()))
          .thenReturn(fileChannel);

      doThrow(InterruptedException.class).when(executorService).invokeAll(anyList());

      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
      verify(segmentFile, times(1)).close();
    }
  }

}
