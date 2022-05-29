package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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
  ExecutorService executorService;

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
      assertEquals(String.valueOf(segmentStartIndex), segment.getSegmentFileKey());
    }
  }
}
