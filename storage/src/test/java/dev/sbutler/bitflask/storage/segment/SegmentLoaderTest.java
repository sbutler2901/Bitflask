package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class SegmentLoaderTest {

  @InjectMocks
  SegmentLoader segmentLoader;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  SegmentFile.Factory segmentFileFactory;
  @Mock
  SegmentFactory segmentFactory;
  @Mock
  StorageConfiguration storageConfiguration;

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(
        Files.class); MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(
        FileChannel.class)) {
      // Arrange
      /// File Paths
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true, true, false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      doReturn(Path.of("0_segment.txt")).when(firstPath).getFileName();
      doReturn(Path.of("1_segment.txt")).when(secondPath).getFileName();
      when(pathIterator.next()).thenReturn(firstPath, secondPath);
      doReturn(pathIterator).when(directoryStream).iterator();
      /// File Paths sorting
      FileTime firstFileTime = FileTime.from(0L, TimeUnit.SECONDS);
      FileTime secondFileTime = FileTime.from(10L, TimeUnit.SECONDS);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime, secondFileTime);
      /// FileChannels
      FileChannel firstFileChannel = mock(FileChannel.class);
      FileChannel secondFileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), anySet()))
          .thenReturn(firstFileChannel, secondFileChannel);
      InOrder fileChannelOrder = inOrder(FileChannel.class);
      /// SegmentFiles
      SegmentFile firstSegmentFile = mock(SegmentFile.class);
      SegmentFile secondSegmentFile = mock(SegmentFile.class);
      when(segmentFileFactory.create(any(), any(), any()))
          .thenReturn(firstSegmentFile, secondSegmentFile);
      /// Segments
      Segment firstSegment = mock(Segment.class);
      Segment secondSegment = mock(Segment.class);
      doReturn(firstSegment).when(segmentFactory).createSegmentFromFile(firstSegmentFile);
      doReturn(secondSegment).when(segmentFactory).createSegmentFromFile(secondSegmentFile);

      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();

      // Assert
      assertEquals(firstSegment, managedSegments.writableSegment());
      assertEquals(1, managedSegments.frozenSegments().size());
      assertEquals(secondSegment, managedSegments.frozenSegments().get(0));
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
    assertEquals(writableSegment, managedSegments.writableSegment());
    assertEquals(0, managedSegments.frozenSegments().size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_noFilesFound() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      /// File Paths
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      doReturn(false).when(pathIterator).hasNext();
      doReturn(pathIterator).when(directoryStream).iterator();
      /// Segments
      doReturn(false).when(segmentFactory).createSegmentStoreDir();
      Segment writableSegment = mock(Segment.class);
      doReturn(writableSegment).when(segmentFactory).createSegment();
      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
      // Assert
      assertEquals(writableSegment, managedSegments.writableSegment());
      assertEquals(0, managedSegments.frozenSegments().size());
    }
  }

  @Test
  void loadExistingSegments_filePaths_directoryIteratorException() {
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
  void loadExistingSegments_fileChannels_executorServiceInterruptedException() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      /// File Paths
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true, false);
      Path firstPath = mock(Path.class);
      doReturn(Path.of("0_segment.txt")).when(firstPath).getFileName();
      doReturn(firstPath).when(pathIterator).next();
      doReturn(pathIterator).when(directoryStream).iterator();
      /// File Paths sorting
      FileTime firstFileTime = FileTime.from(0L, TimeUnit.SECONDS);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime);
      /// FileChannels
      doThrow(InterruptedException.class).when(executorService)
          .invokeAll(ArgumentMatchers.<ImmutableList<Callable<FileChannel>>>any());
      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_fileChannels_futureFailure() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      /// File Paths
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true, true, true, true, false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      Path thirdPath = mock(Path.class);
      Path fourthPath = mock(Path.class);
      doReturn(Path.of("0_segment.txt")).when(firstPath).getFileName();
      doReturn(Path.of("1_segment.txt")).when(secondPath).getFileName();
      doReturn(Path.of("2_segment.txt")).when(thirdPath).getFileName();
      doReturn(Path.of("3_segment.txt")).when(fourthPath).getFileName();
      when(pathIterator.next()).thenReturn(firstPath, secondPath, thirdPath, fourthPath);
      doReturn(pathIterator).when(directoryStream).iterator();
      /// File Paths sorting
      FileTime firstFileTime = FileTime.from(0L, TimeUnit.SECONDS);
      FileTime secondFileTime = FileTime.from(5L, TimeUnit.SECONDS);
      FileTime thirdFileTime = FileTime.from(10L, TimeUnit.SECONDS);
      FileTime fourthFileTime = FileTime.from(15L, TimeUnit.SECONDS);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime, secondFileTime, thirdFileTime, fourthFileTime);
      /// FileChannels
      Future<FileChannel> firstFuture = mock(Future.class);
      doThrow(InterruptedException.class).when(firstFuture).get();
      Future<FileChannel> secondFuture = mock(Future.class);
      doThrow(new ExecutionException(new IOException("test"))).when(secondFuture).get();
      Future<FileChannel> thirdFuture = mock(Future.class);
      FileChannel thirdFileChannel = mock(FileChannel.class);
      doReturn(thirdFileChannel).when(thirdFuture).get();
      Future<FileChannel> fourthFuture = mock(Future.class);
      FileChannel fourthFileChannel = mock(FileChannel.class);
      doThrow(IOException.class).when(fourthFileChannel).close();
      doReturn(fourthFileChannel).when(fourthFuture).get();
      doReturn(ImmutableList.of(firstFuture, secondFuture, thirdFuture, fourthFuture))
          .when(executorService).invokeAll(any());
      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
      verify(thirdFileChannel, times(1)).close();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_segments_executorServiceInterruptedException() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(
        Files.class); MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(
        FileChannel.class)) {
      // Arrange
      /// File Paths
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true, true, false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      doReturn(Path.of("0_segment.txt")).when(firstPath).getFileName();
      doReturn(Path.of("1_segment.txt")).when(secondPath).getFileName();
      when(pathIterator.next()).thenReturn(firstPath, secondPath);
      doReturn(pathIterator).when(directoryStream).iterator();
      /// File Paths sorting
      FileTime firstFileTime = FileTime.from(0L, TimeUnit.SECONDS);
      FileTime secondFileTime = FileTime.from(10L, TimeUnit.SECONDS);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime, secondFileTime);
      /// FileChannels
      FileChannel firstFileChannel = mock(FileChannel.class);
      FileChannel secondFileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), anySet()))
          .thenReturn(firstFileChannel, secondFileChannel);
      /// SegmentFiles
      SegmentFile firstSegmentFile = mock(SegmentFile.class);
      SegmentFile secondSegmentFile = mock(SegmentFile.class);
      when(segmentFileFactory.create(any(), any(), any()))
          .thenReturn(firstSegmentFile, secondSegmentFile);
      /// Segments
      when(executorService.invokeAll(anyList()))
          .thenCallRealMethod()
          .thenThrow(InterruptedException.class);
      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
      verify(firstSegmentFile, times(1)).close();
      verify(secondSegmentFile, times(1)).close();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadExistingSegments_segments_futureFailure() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(
        Files.class); MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(
        FileChannel.class)) {
      // Arrange
      /// File Paths
      DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
      filesMockedStatic.when(() -> Files.newDirectoryStream(any())).thenReturn(directoryStream);
      Iterator<Path> pathIterator = mock(Iterator.class);
      when(pathIterator.hasNext()).thenReturn(true, true, false);
      Path firstPath = mock(Path.class);
      Path secondPath = mock(Path.class);
      doReturn(Path.of("0_segment.txt")).when(firstPath).getFileName();
      doReturn(Path.of("1_segment.txt")).when(secondPath).getFileName();
      when(pathIterator.next()).thenReturn(firstPath, secondPath);
      doReturn(pathIterator).when(directoryStream).iterator();
      /// File Paths sorting
      FileTime firstFileTime = FileTime.from(0L, TimeUnit.SECONDS);
      FileTime secondFileTime = FileTime.from(10L, TimeUnit.SECONDS);
      filesMockedStatic.when(() -> Files.getLastModifiedTime(any(), any()))
          .thenReturn(firstFileTime, secondFileTime);
      /// FileChannels
      FileChannel firstFileChannel = mock(FileChannel.class);
      FileChannel secondFileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(), anySet()))
          .thenReturn(firstFileChannel, secondFileChannel);
      /// SegmentFiles
      SegmentFile firstSegmentFile = mock(SegmentFile.class);
      SegmentFile secondSegmentFile = mock(SegmentFile.class);
      when(segmentFileFactory.create(any(), any(), any()))
          .thenReturn(firstSegmentFile, secondSegmentFile);
      /// Segments
      Future<Segment> firstFuture = mock(Future.class);
      doThrow(InterruptedException.class).when(firstFuture).get();
      Future<Segment> secondFuture = mock(Future.class);
      doThrow(new ExecutionException(new IOException("test"))).when(secondFuture).get();
      OngoingStubbing<List<Future<Segment>>> executorStub = when(
          executorService.invokeAll(anyList()));
      executorStub.thenCallRealMethod().thenReturn(ImmutableList.of(firstFuture, secondFuture));

      // Act / Assert
      assertThrows(IOException.class, () -> segmentLoader.loadExistingSegments());
      verify(firstSegmentFile, times(1)).close();
      verify(secondSegmentFile, times(1)).close();
    }
  }

}
