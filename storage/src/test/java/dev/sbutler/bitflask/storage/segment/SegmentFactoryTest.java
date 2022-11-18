package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

public class SegmentFactoryTest {

  private SegmentFactory segmentFactory;
  private SegmentFile.Factory segmentFileFactory;

  @Test
  @SuppressWarnings({"unchecked"})
  void createSegment_createMode() throws Exception {
    mockSegmentFactory(StandardOpenOption.CREATE);
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      // Arrange
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      SegmentFile segmentFile = mock(SegmentFile.class);
      when(segmentFileFactory.create(any(), any(), any())).thenReturn(segmentFile);
      // Act
      Segment segment = segmentFactory.createSegment();
      // Assert
      assertThat(segment.exceedsStorageThreshold()).isFalse();
      verify(segmentFile, times(0)).truncate(anyLong());
    }
  }

  @Test
  @SuppressWarnings({"unchecked"})
  void createSegment_truncateMode() throws Exception {
    mockSegmentFactory(StandardOpenOption.TRUNCATE_EXISTING);
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      // Arrange
      FileChannel fileChannel = mock(FileChannel.class);
      ArgumentCaptor<ImmutableSet<StandardOpenOption>> openOptionsCaptor = ArgumentCaptor.forClass(
          ImmutableSet.class);
      fileChannelMockedStatic.when(
              () -> FileChannel.open(any(Path.class), openOptionsCaptor.capture()))
          .thenReturn(fileChannel);
      SegmentFile segmentFile = mock(SegmentFile.class);
      when(segmentFileFactory.create(any(), any(), any())).thenReturn(segmentFile);
      // Act
      Segment segment = segmentFactory.createSegment();
      // Assert
      assertThat(segment.exceedsStorageThreshold()).isFalse();
      verify(segmentFile, times(1)).truncate(anyLong());
    }
  }

  @Test
  void createSegment_createMode_SegmentFileProvided_valuePresent() throws Exception {
    // Arrange
    mockSegmentFactory(StandardOpenOption.CREATE);
    String key = "a";
    SegmentFile segmentFile = mock(SegmentFile.class);
    when(segmentFile.size()).thenReturn(5L);
    when(segmentFile.readByte(anyLong()))
        .thenReturn((byte) 1)
        .thenReturn((byte) 0) // key_value header
        .thenReturn((byte) 1);
    when(segmentFile.readAsString(anyInt(), anyLong())).thenReturn(key);
    // Act
    Segment segment = segmentFactory.createSegmentFromFile(segmentFile);
    // Assert
    assertThat(segment.containsKey(key)).isTrue();
    verify(segmentFile, times(3)).readByte(anyLong());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
    verify(segmentFile, times(0)).truncate(anyLong());
  }

  @Test
  void createSegment_truncateMode_SegmentFileProvided_valuePresent() throws Exception {
    // Arrange
    mockSegmentFactory(StandardOpenOption.TRUNCATE_EXISTING);
    String key = "a";
    SegmentFile segmentFile = mock(SegmentFile.class);
    when(segmentFile.size()).thenReturn(5L);
    when(segmentFile.readByte(anyLong()))
        .thenReturn((byte) 1)
        .thenReturn((byte) 0) // key_value header
        .thenReturn((byte) 1);
    when(segmentFile.readAsString(anyInt(), anyLong())).thenReturn(key);
    // Act
    Segment segment = segmentFactory.createSegmentFromFile(segmentFile);
    // Assert
    assertThat(segment.containsKey(key)).isTrue();
    verify(segmentFile, times(3)).readByte(anyLong());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
    verify(segmentFile, times(1)).truncate(anyLong());
  }

  @Test
  void getFileChannelOptions() {
    // Arrange
    mockSegmentFactory(StandardOpenOption.CREATE);
    // Act
    ImmutableSet<StandardOpenOption> openOptions = segmentFactory.getFileChannelOptions();
    // Assert
    assertThat(openOptions).containsExactly(StandardOpenOption.READ, StandardOpenOption.WRITE,
        StandardOpenOption.CREATE);
  }

  @Test
  void createSegmentStoreDir_CreateDirectory() throws Exception {
    mockSegmentFactory(StandardOpenOption.CREATE);
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      filesMockedStatic.when(() -> Files.isDirectory(any(Path.class))).thenReturn(false);
      // Act / Assert
      assertThat(segmentFactory.createSegmentStoreDir()).isTrue();
      filesMockedStatic.verify(() -> Files.createDirectories(any(Path.class)), times(1));
    }
  }

  @Test
  void createSegmentStoreDir_DirectoryExists() throws Exception {
    mockSegmentFactory(StandardOpenOption.CREATE);
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      filesMockedStatic.when(() -> Files.isDirectory(any(Path.class))).thenReturn(true);
      // Act / Assert
      assertThat(segmentFactory.createSegmentStoreDir()).isFalse();
      filesMockedStatic.verify(() -> Files.createDirectories(any(Path.class)), times(0));
    }
  }

  private void mockSegmentFactory(StandardOpenOption openOption) {
    segmentFileFactory = mock(SegmentFile.Factory.class);
    StorageConfigurations storageConfigurations = mock(StorageConfigurations.class);
    when(storageConfigurations.getStorageStoreDirectoryPath())
        .thenReturn(Path.of("/tmp/.bitflask"));
    when(storageConfigurations.getStorageSegmentSizeLimit()).thenReturn(100L);
    when(storageConfigurations.getStorageSegmentCreationMode()).thenReturn(openOption);
    segmentFactory = new SegmentFactory(segmentFileFactory, storageConfigurations);
  }
}
