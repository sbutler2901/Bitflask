package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
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
  void createSegment() throws IOException {
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      Segment segment = segmentFactory.createSegment();
      assertFalse(segment.exceedsStorageThreshold());
    }
  }

  @Test
  void setSegmentStartIndex() throws IOException {
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      int segmentStartIndex = 10;
      segmentFactory.setSegmentStartIndex(segmentStartIndex);
      Segment segment = segmentFactory.createSegment();
      assertEquals(String.valueOf(segmentStartIndex), segment.getSegmentFileKey());
    }
  }
}
