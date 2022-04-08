package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class SegmentManagerImplTest {

  ExecutorService executorService = mock(ExecutorService.class);

  @Test
  void getActiveSegment() throws IOException {
    try (MockedConstruction<SegmentFile> segmentFileMockedConstruction = mockConstruction(
        SegmentFile.class);
        MockedConstruction<SegmentImpl> segmentMockedConstruction = mockConstruction(
            SegmentImpl.class)
    ) {
      SegmentManagerImpl segmentManager = new SegmentManagerImpl(executorService);

      SegmentImpl segment = segmentMockedConstruction.constructed().get(0);
      doReturn(false).when(segment).exceedsStorageThreshold();

      assertEquals(segment, segmentManager.getActiveSegment());
    }
  }

  @Test
  void getActiveSegment_thresholdExceeded() throws IOException {
    try (MockedConstruction<SegmentFile> segmentFileMockedConstruction = mockConstruction(
        SegmentFile.class);
        MockedConstruction<SegmentImpl> segmentMockedConstruction = mockConstruction(
            SegmentImpl.class)
    ) {
      SegmentManagerImpl segmentManager = new SegmentManagerImpl(executorService);

      SegmentImpl segmentFirst = segmentMockedConstruction.constructed().get(0);
      doReturn(true).when(segmentFirst).exceedsStorageThreshold();

      SegmentImpl active = segmentManager.getActiveSegment();

      assertEquals(2, segmentFileMockedConstruction.constructed().size());
      assertEquals(2, segmentMockedConstruction.constructed().size());
      SegmentImpl segmentSecond = segmentMockedConstruction.constructed().get(1);
      assertEquals(segmentSecond, active);
    }
  }

  @Test
  void getStorageSegmentsIterator_protected() throws IOException {
    // Ensure Manager's underlying collection cannot be modified via published iterator
    try (MockedConstruction<SegmentFile> segmentFileMockedConstruction = mockConstruction(
        SegmentFile.class);
        MockedConstruction<SegmentImpl> segmentMockedConstruction = mockConstruction(
            SegmentImpl.class)
    ) {
      SegmentManagerImpl segmentManager = new SegmentManagerImpl(executorService);

      Iterator<SegmentImpl> storageSegmentIterator = segmentManager.getStorageSegmentsIterator();
      while (storageSegmentIterator.hasNext()) {
        storageSegmentIterator.next();
        storageSegmentIterator.remove();
      }

      try {
        segmentManager.getActiveSegment();
      } catch (NoSuchElementException e) {
        fail("SegmentManagerImpl's collection should not be modifiable via published iterator");
      }
    }
  }
}
