package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class SegmentFactoryImplTest {

  @InjectMocks
  SegmentFactoryImpl segmentFactory;

  @BeforeEach
  void beforeEach() {
    segmentFactory.logger = mock(Logger.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createSegment() throws IOException {
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      doReturn(0L).when(fileChannel).size();

      Segment segment = segmentFactory.createSegment();
      assertFalse(segment.exceedsStorageThreshold());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void setSegmentStartIndex() throws IOException {
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      doReturn(0L).when(fileChannel).size();

      int segmentStartIndex = 10;
      segmentFactory.setSegmentStartIndex(segmentStartIndex);
      Segment segment = segmentFactory.createSegment();
      assertEquals(segmentStartIndex, segment.getSegmentFileKey());
    }
  }

  @Test
  void createSegmentStoreDir_CreateDirectory() throws IOException {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.isDirectory(any(Path.class))).thenReturn(false);
      assertTrue(segmentFactory.createSegmentStoreDir());
      filesMockedStatic.verify(() -> Files.createDirectories(any(Path.class)), times(1));
    }
  }

  @Test
  void createSegmentStoreDir_DirectoryExists() throws IOException {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.isDirectory(any(Path.class))).thenReturn(true);
      assertFalse(segmentFactory.createSegmentStoreDir());
      filesMockedStatic.verify(() -> Files.createDirectories(any(Path.class)), times(0));
    }
  }

  @Test
  void getSegmentKeyFromPath() {
    Path path;
    path = Path.of(String.format(SegmentFactoryImpl.DEFAULT_SEGMENT_FILENAME, 0));
    assertEquals(0, segmentFactory.getSegmentKeyFromPath(path));
    path = Path.of(String.format(SegmentFactoryImpl.DEFAULT_SEGMENT_FILENAME, 10));
    assertEquals(10, segmentFactory.getSegmentKeyFromPath(path));
  }
}
