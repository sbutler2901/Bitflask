package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentFactoryImplTest {

  @InjectMocks
  SegmentFactoryImpl segmentFactory;
  @Mock
  SegmentFileFactory segmentFileFactory;

  @Test
  @SuppressWarnings("unchecked")
  void createSegment() throws IOException {
    try (MockedStatic<FileChannel> fileChannelMockedStatic = mockStatic(FileChannel.class)) {
      FileChannel fileChannel = mock(FileChannel.class);
      fileChannelMockedStatic.when(() -> FileChannel.open(any(Path.class), any(Set.class)))
          .thenReturn(fileChannel);
      doReturn(mock(SegmentFile.class)).when(segmentFileFactory).create(any(), any(), anyInt());

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
      SegmentFile segmentFile = mock(SegmentFile.class);
      doReturn(segmentFile).when(segmentFileFactory).create(any(), any(), anyInt());
      int segmentStartKey = 10;
      doReturn(segmentStartKey).when(segmentFile).getSegmentFileKey();
      segmentFactory.setSegmentStartKey(segmentStartKey);
      Segment segment = segmentFactory.createSegment();
      assertEquals(segmentStartKey, segment.getSegmentFileKey());
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
