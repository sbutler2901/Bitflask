package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class SegmentManagerImplTest {

  ExecutorService executorService = mock(ExecutorService.class);

  @Test
  void getActiveSegment() throws IOException {
    try (
        MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
            AsynchronousFileChannel.class);
        MockedConstruction<SegmentFile> segmentFileMockedConstruction = mockConstruction(
            SegmentFile.class);
        MockedConstruction<SegmentImpl> segmentMockedConstruction = mockConstruction(
            SegmentImpl.class)
    ) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(() ->
              AsynchronousFileChannel.open(any(), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      SegmentManagerImpl segmentManager = new SegmentManagerImpl(executorService);

      Segment segment = segmentMockedConstruction.constructed().get(0);
      doReturn(false).when(segment).exceedsStorageThreshold();

      assertEquals(segment, segmentManager.getActiveSegment());
    }
  }

  @Test
  void getActiveSegment_thresholdExceeded() throws IOException {
    try (
        MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
            AsynchronousFileChannel.class);
        MockedConstruction<SegmentFile> segmentFileMockedConstruction = mockConstruction(
            SegmentFile.class);
        MockedConstruction<SegmentImpl> segmentMockedConstruction = mockConstruction(
            SegmentImpl.class)
    ) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(() ->
              AsynchronousFileChannel.open(any(), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      SegmentManagerImpl segmentManager = new SegmentManagerImpl(executorService);

      Segment segmentFirst = segmentMockedConstruction.constructed().get(0);
      doReturn(true).when(segmentFirst).exceedsStorageThreshold();

      Segment active = segmentManager.getActiveSegment();

      assertEquals(2, segmentFileMockedConstruction.constructed().size());
      assertEquals(2, segmentMockedConstruction.constructed().size());
      Segment segmentSecond = segmentMockedConstruction.constructed().get(1);
      assertEquals(segmentSecond, active);
    }
  }

  @Test
  void getSegmentsIterator_protected() throws IOException {
    // Ensure Manager's underlying collection cannot be modified via published iterator
    try (
        MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
            AsynchronousFileChannel.class);
        MockedConstruction<SegmentFile> segmentFileMockedConstruction = mockConstruction(
            SegmentFile.class);
        MockedConstruction<SegmentImpl> segmentMockedConstruction = mockConstruction(
            SegmentImpl.class)
    ) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(() ->
              AsynchronousFileChannel.open(any(), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      SegmentManagerImpl segmentManager = new SegmentManagerImpl(executorService);
      Iterator<Segment> segmentsIterator = segmentManager.getSegmentsIterator();
      segmentsIterator.next();
      assertThrows(UnsupportedOperationException.class, segmentsIterator::remove);
    }
  }
}
