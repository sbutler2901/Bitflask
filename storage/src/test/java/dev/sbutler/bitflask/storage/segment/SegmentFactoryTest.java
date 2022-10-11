package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentFactoryTest {

  SegmentFactory segmentFactory;
  SegmentFile.Factory segmentFileFactory;
  StorageConfiguration storageConfiguration;

  @BeforeEach
  void beforeEach() {
    segmentFileFactory = mock(SegmentFile.Factory.class);
    storageConfiguration = mock(StorageConfiguration.class);
    doReturn(Path.of("/tmp/.bitflask")).when(storageConfiguration).getStorageStoreDirectoryPath();
    doReturn(100L).when(storageConfiguration).getStorageSegmentSizeLimit();
    doReturn(StandardOpenOption.CREATE).when(storageConfiguration).getStorageSegmentCreationMode();
    segmentFactory = new SegmentFactory(segmentFileFactory, storageConfiguration);
  }

  @Test
  @SuppressWarnings({"unchecked"})
  void createSegment_createMode() throws Exception {
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      // Arrange
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      doReturn(mock(SegmentFile.class)).when(segmentFileFactory).create(any(), any(), anyInt());
      // Act
      Segment segment = segmentFactory.createSegment();
      // Assert
      assertFalse(segment.exceedsStorageThreshold());
    }
  }

  @Test
  @SuppressWarnings({"unchecked"})
  void createSegment_truncateMode() throws Exception {
    // TODO: implement with truncation
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      // Arrange
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      doReturn(mock(SegmentFile.class)).when(segmentFileFactory).create(any(), any(), anyInt());
      // Act
      Segment segment = segmentFactory.createSegment();
      // Assert
      assertFalse(segment.exceedsStorageThreshold());
    }
  }

  @Test
  void createSegment_createMode_SegmentFileProvided_valuePresent() throws Exception {
    // Arrange
    String key = "a";
    SegmentFile segmentFile = mock(SegmentFile.class);
    doReturn(5L).when(segmentFile).size();
    when(segmentFile.readByte(anyLong()))
        .thenReturn((byte) 1)
        .thenReturn((byte) 0) // key_value header
        .thenReturn((byte) 1);
    doReturn(key).when(segmentFile).readAsString(anyInt(), anyLong());
    // Act
    Segment segment = segmentFactory.createSegmentFromFile(segmentFile);
    // Assert
    assertTrue(segment.containsKey(key));
    verify(segmentFile, times(3)).readByte(anyLong());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void createSegment_truncateMode_SegmentFileProvided_valuePresent() throws Exception {
    // TODO: implement with truncation
    // Arrange
    String key = "a";
    SegmentFile segmentFile = mock(SegmentFile.class);
    doReturn(5L).when(segmentFile).size();
    when(segmentFile.readByte(anyLong()))
        .thenReturn((byte) 1)
        .thenReturn((byte) 0) // key_value header
        .thenReturn((byte) 1);
    doReturn(key).when(segmentFile).readAsString(anyInt(), anyLong());
    // Act
    Segment segment = segmentFactory.createSegmentFromFile(segmentFile);
    // Assert
    assertTrue(segment.containsKey(key));
    verify(segmentFile, times(3)).readByte(anyLong());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  @SuppressWarnings({"unchecked"})
  void setSegmentStartIndex() throws Exception {
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      // Arrange
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      SegmentFile segmentFile = mock(SegmentFile.class);
      doReturn(segmentFile).when(segmentFileFactory).create(any(), any(), anyInt());
      int segmentStartKey = 10;
      doReturn(segmentStartKey).when(segmentFile).getSegmentFileKey();
      segmentFactory.setSegmentStartKey(segmentStartKey);
      // Act
      Segment segment = segmentFactory.createSegment();
      // Assert
      assertEquals(segmentStartKey, segment.getSegmentFileKey());
    }
  }

  @Test
  void createSegmentStoreDir_CreateDirectory() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      filesMockedStatic.when(() -> Files.isDirectory(any(Path.class))).thenReturn(false);
      // Act / Assert
      assertTrue(segmentFactory.createSegmentStoreDir());
      filesMockedStatic.verify(() -> Files.createDirectories(any(Path.class)), times(1));
    }
  }

  @Test
  void createSegmentStoreDir_DirectoryExists() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Arrange
      filesMockedStatic.when(() -> Files.isDirectory(any(Path.class))).thenReturn(true);
      // Act / Assert
      assertFalse(segmentFactory.createSegmentStoreDir());
      filesMockedStatic.verify(() -> Files.createDirectories(any(Path.class)), times(0));
    }
  }

  @Test
  void getSegmentKeyFromPath() {
    Path path;
    // Arrange
    path = Path.of(String.format(SegmentFactory.DEFAULT_SEGMENT_FILENAME, 0));
    // Act / Assert
    assertEquals(0, segmentFactory.getSegmentKeyFromPath(path));
    // Arrange
    path = Path.of(String.format(SegmentFactory.DEFAULT_SEGMENT_FILENAME, 10));
    // Act / Assert
    assertEquals(10, segmentFactory.getSegmentKeyFromPath(path));
  }
}
