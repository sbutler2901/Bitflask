package dev.sbutler.bitflask.storage.segmentV1;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.segmentV1.SegmentFile.Header;
import dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
@ExtendWith(MockitoExtension.class)
class SegmentLoaderTest {

  @InjectMocks
  private SegmentLoader segmentLoader;
  @Spy
  private ThreadFactory threadFactory = Thread.ofVirtual().factory();
  @Mock
  private SegmentFactory segmentFactory;
  @Mock
  private SegmentFile.Factory segmentFileFactory;
  @Mock
  private FilesHelper filesHelper;
  @Mock
  private StorageConfigurations storageConfigurations;

  @Test
  void loadExistingSegments() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class);
        MockedStatic<Header> headerMockedStatic = mockStatic(Header.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path0 = Path.of("0_segment.txt");
      Path path1 = Path.of("1_segment.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path0, path1));
      /// modified time
      SettableFuture<FileTime> timeFuture0 = SettableFuture.create();
      timeFuture0.set(FileTime.from(Instant.now()));
      SettableFuture<FileTime> timeFuture1 = SettableFuture.create();
      timeFuture1.set(FileTime.from(Instant.now()));
      ImmutableMap<Path, Future<FileTime>> fileTimeFutures =
          ImmutableMap.of(path0, timeFuture0, path1, timeFuture1);
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenReturn(fileTimeFutures);
      /// FileChannel
      SettableFuture<FileChannel> fcFuture0 = SettableFuture.create();
      fcFuture0.set(mock(FileChannel.class));
      SettableFuture<FileChannel> fcFuture1 = SettableFuture.create();
      fcFuture1.set(mock(FileChannel.class));
      ImmutableMap<Path, Future<FileChannel>> fileChannelFutures =
          ImmutableMap.of(path1, fcFuture1, path0, fcFuture0);
      ArgumentCaptor<ImmutableList<Path>> sortedFilePathsCaptor = ArgumentCaptor.forClass(
          ImmutableList.class);
      when(filesHelper.openFileChannels(sortedFilePathsCaptor.capture(), any()))
          .thenReturn(fileChannelFutures);
      /// SegmentFiles
      headerMockedStatic.when(() -> Header.readHeaderFromFileChannel(any()))
          .thenReturn(new Header(1))
          .thenReturn(new Header(0));
      /// Segments
      Segment segment0 = mock(Segment.class);
      Segment segment1 = mock(Segment.class);
      when(segmentFactory.createSegmentFromFile(any()))
          .thenReturn(segment1)
          .thenReturn(segment0);
      when(segmentFileFactory.create(any(), any(), any()))
          .thenReturn(mock(SegmentFile.class))
          .thenReturn(mock(SegmentFile.class));
      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
      // Assert
      assertThat(managedSegments.writableSegment()).isEqualTo(segment1);
      assertThat(managedSegments.frozenSegments()).hasSize(1);
      assertThat(managedSegments.frozenSegments().get(0)).isEqualTo(segment0);
      /// modified time path sorting
      ImmutableList<Path> pathsSorted = sortedFilePathsCaptor.getValue();
      assertThat(pathsSorted).hasSize(2);
      assertThat(pathsSorted).containsExactly(path1, path0).inOrder();
    }
  }

  @Test
  void loadExistingSegments_directoryCreated() throws Exception {
    // Arrange
    /// Store dir
    when(segmentFactory.createSegmentStoreDir()).thenReturn(true);
    /// Segments
    Segment writableSegment = mock(Segment.class);
    when(segmentFactory.createSegment()).thenReturn(writableSegment);
    // Act
    ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
    // Assert
    assertThat(managedSegments.writableSegment()).isEqualTo(writableSegment);
    assertThat(managedSegments.frozenSegments()).hasSize(0);
  }

  @Test
  void loadExistingSegments_filePaths_noFilesFound() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of());
      /// Segments
      Segment writableSegment = mock(Segment.class);
      doReturn(writableSegment).when(segmentFactory).createSegment();
      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
      // Assert
      assertThat(managedSegments.writableSegment()).isEqualTo(writableSegment);
      assertThat(managedSegments.frozenSegments()).hasSize(0);
    }
  }

  @Test
  void loadExistingSegments_filePaths_invalidSegmentFilePath() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path = Path.of("invalid.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path));
      /// Segments
      Segment writableSegment = mock(Segment.class);
      doReturn(writableSegment).when(segmentFactory).createSegment();
      // Act
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
      // Assert
      assertThat(managedSegments.writableSegment()).isEqualTo(writableSegment);
      assertThat(managedSegments.frozenSegments()).hasSize(0);
    }
  }

  @Test
  void loadExistingSegments_filePaths_ioException() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      IOException ioException = new IOException("test");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any())).thenThrow(ioException);
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasCauseThat().isEqualTo(ioException);
      assertThat(exception).hasMessageThat().ignoringCase().contains("store directory");
    }
  }

  @Test
  void loadExistingSegments_sortModifiedFirst_interrupted() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(Path.of("0_segment.txt")));
      /// modified time
      InterruptedException interruptedException = new InterruptedException("test");
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenThrow(interruptedException);
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasCauseThat().isEqualTo(interruptedException);
      assertThat(exception).hasMessageThat().ignoringCase().contains("modified time");
      assertThat(exception).hasMessageThat().ignoringCase().contains("interrupted");
    }
  }

  @Test
  void loadExistingSegments_sortModifiedFirst_failedFutures() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path = Path.of("0_segment.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path));
      /// modified time
      SettableFuture<FileTime> timeFuture = SettableFuture.create();
      IOException ioException = new IOException("test");
      timeFuture.setException(ioException);
      ImmutableMap<Path, Future<FileTime>> fileTimeFutures = ImmutableMap.of(path, timeFuture);
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenReturn(fileTimeFutures);
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasMessageThat().ignoringCase().contains("modified time");
      assertThat(exception).hasMessageThat().ignoringCase().contains("failures");
    }
  }

  @Test
  void loadExistingSegments_fileChannels_interrupted() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path = Path.of("0_segment.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path));
      /// modified time
      SettableFuture<FileTime> timeFuture = SettableFuture.create();
      timeFuture.set(FileTime.from(Instant.now()));
      ImmutableMap<Path, Future<FileTime>> fileTimeFutures = ImmutableMap.of(path, timeFuture);
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenReturn(fileTimeFutures);
      /// FileChannel
      InterruptedException interruptedException = new InterruptedException("test");
      when(filesHelper.openFileChannels(any(), any())).thenThrow(interruptedException);
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasCauseThat().isEqualTo(interruptedException);
      assertThat(exception).hasMessageThat().ignoringCase().contains("opening file channel");
      assertThat(exception).hasMessageThat().ignoringCase().contains("interrupted");
    }
  }

  @Test
  void loadExistingSegments_fileChannels_failedFutures() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path = Path.of("0_segment.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path));
      /// modified time
      SettableFuture<FileTime> timeFuture = SettableFuture.create();
      timeFuture.set(FileTime.from(Instant.now()));
      ImmutableMap<Path, Future<FileTime>> fileTimeFutures = ImmutableMap.of(path, timeFuture);
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenReturn(fileTimeFutures);
      /// FileChannel
      SettableFuture<FileChannel> fcFuture = SettableFuture.create();
      IOException ioException = new IOException("test");
      fcFuture.setException(ioException);
      when(filesHelper.openFileChannels(any(), any()))
          .thenReturn(ImmutableMap.of(path, fcFuture));
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasMessageThat().ignoringCase().contains("failed opening file channel");
      verify(filesHelper, times(1)).closeFileChannelsBestEffort(any());
    }
  }

  @Test
  void loadExistingSegments_segmentFiles_header_ioException() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class);
        MockedStatic<Header> headerMockedStatic = mockStatic(Header.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path = Path.of("0_segment.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path));
      /// modified time
      SettableFuture<FileTime> timeFuture = SettableFuture.create();
      timeFuture.set(FileTime.from(Instant.now()));
      ImmutableMap<Path, Future<FileTime>> fileTimeFutures = ImmutableMap.of(path, timeFuture);
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenReturn(fileTimeFutures);
      /// FileChannel
      SettableFuture<FileChannel> fcFuture = SettableFuture.create();
      fcFuture.set(mock(FileChannel.class));
      ImmutableMap<Path, Future<FileChannel>> fileChannelFutures = ImmutableMap.of(path, fcFuture);
      when(filesHelper.openFileChannels(any(), any())).thenReturn(fileChannelFutures);
      /// SegmentFiles
      IOException ioException = new IOException("test");
      headerMockedStatic.when(() -> Header.readHeaderFromFileChannel(any()))
          .thenThrow(ioException);
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasCauseThat().isEqualTo(ioException);
      assertThat(exception).hasMessageThat().ignoringCase().contains("SegmentFile.Header");
    }
  }

  @Test
  void loadExistingSegments_segments_failedFutures() throws Exception {
    try (MockedStatic<MoreFiles> filesMockedStatic = mockStatic(MoreFiles.class);
        MockedStatic<Header> headerMockedStatic = mockStatic(Header.class)) {
      // Arrange
      /// Store dir
      when(segmentFactory.createSegmentStoreDir()).thenReturn(false);
      /// File Paths
      Path path = Path.of("0_segment.txt");
      filesMockedStatic.when(() -> MoreFiles.listFiles(any()))
          .thenReturn(ImmutableList.of(path));
      /// modified time
      SettableFuture<FileTime> timeFuture = SettableFuture.create();
      timeFuture.set(FileTime.from(Instant.now()));
      ImmutableMap<Path, Future<FileTime>> fileTimeFutures = ImmutableMap.of(path, timeFuture);
      when(filesHelper.getLastModifiedTimeOfFiles(any())).thenReturn(fileTimeFutures);
      /// FileChannel
      SettableFuture<FileChannel> fcFuture = SettableFuture.create();
      fcFuture.set(mock(FileChannel.class));
      ImmutableMap<Path, Future<FileChannel>> fileChannelFutures = ImmutableMap.of(path, fcFuture);
      when(filesHelper.openFileChannels(any(), any())).thenReturn(fileChannelFutures);
      /// SegmentFiles
      headerMockedStatic.when(() -> Header.readHeaderFromFileChannel(any()))
          .thenReturn(new Header(0));
      SegmentFile segmentFile = mock(SegmentFile.class);
      when(segmentFileFactory.create(any(), any(), any()))
          .thenReturn(segmentFile);
      /// Segments
      IOException ioException = new IOException("test");
      when(segmentFactory.createSegmentFromFile(any()))
          .thenThrow(ioException);
      // Act
      SegmentLoaderException exception =
          assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
      // Assert
      assertThat(exception).hasMessageThat().ignoringCase().contains("creating segments");
      verify(segmentFile, times(1)).close();
    }
  }

  @Test
  void loadExistingSegments_managedSegments_ioException() throws Exception {
    // Arrange
    /// Store dir
    when(segmentFactory.createSegmentStoreDir()).thenReturn(true);
    /// Segments
    IOException ioException = new IOException("test");
    when(segmentFactory.createSegment()).thenThrow(ioException);
    // Act
    SegmentLoaderException exception =
        assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
    // Assert
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat().ignoringCase().contains("ManagedSegments");
    assertThat(exception).hasMessageThat().ignoringCase().contains("new segment");
  }

  @Test
  void loadExistingSegments_uncaughtExceptionWrapping() throws Exception {
    // Arrange
    IOException ioException = new IOException("test");
    when(segmentFactory.createSegmentStoreDir())
        .thenThrow(ioException);
    // Act
    SegmentLoaderException exception =
        assertThrows(SegmentLoaderException.class, () -> segmentLoader.loadExistingSegments());
    // Assert
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat().ignoringCase()
        .contains("failed to load existing segments");
  }
}
