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

public class StorageSegmentManagerTest {

  ExecutorService executorService = mock(ExecutorService.class);

  @Test
  void getActiveSegment() throws IOException {
    try (MockedConstruction<StorageSegmentFile> segmentFileMockedConstruction = mockConstruction(
        StorageSegmentFile.class);
        MockedConstruction<StorageSegment> segmentMockedConstruction = mockConstruction(
            StorageSegment.class)
    ) {
      StorageSegmentManager storageSegmentManager = new StorageSegmentManager(executorService);

      StorageSegment storageSegment = segmentMockedConstruction.constructed().get(0);
      doReturn(false).when(storageSegment).exceedsStorageThreshold();

      assertEquals(storageSegment, storageSegmentManager.getActiveSegment());
    }
  }

  @Test
  void getActiveSegment_thresholdExceeded() throws IOException {
    try (MockedConstruction<StorageSegmentFile> segmentFileMockedConstruction = mockConstruction(
        StorageSegmentFile.class);
        MockedConstruction<StorageSegment> segmentMockedConstruction = mockConstruction(
            StorageSegment.class)
    ) {
      StorageSegmentManager storageSegmentManager = new StorageSegmentManager(executorService);

      StorageSegment storageSegmentFirst = segmentMockedConstruction.constructed().get(0);
      doReturn(true).when(storageSegmentFirst).exceedsStorageThreshold();

      StorageSegment active = storageSegmentManager.getActiveSegment();

      assertEquals(2, segmentFileMockedConstruction.constructed().size());
      assertEquals(2, segmentMockedConstruction.constructed().size());
      StorageSegment storageSegmentSecond = segmentMockedConstruction.constructed().get(1);
      assertEquals(storageSegmentSecond, active);
    }
  }

  @Test
  void getStorageSegmentsIterator_protected() throws IOException {
    // Ensure Manager's underlying collection cannot be modified via published iterator
    try (MockedConstruction<StorageSegmentFile> segmentFileMockedConstruction = mockConstruction(
        StorageSegmentFile.class);
        MockedConstruction<StorageSegment> segmentMockedConstruction = mockConstruction(
            StorageSegment.class)
    ) {
      StorageSegmentManager storageSegmentManager = new StorageSegmentManager(executorService);

      Iterator<StorageSegment> storageSegmentIterator = storageSegmentManager.getStorageSegmentsIterator();
      while (storageSegmentIterator.hasNext()) {
        storageSegmentIterator.next();
        storageSegmentIterator.remove();
      }

      try {
        storageSegmentManager.getActiveSegment();
      } catch (NoSuchElementException e) {
        fail("StorageSegmentManager's collection should not be modifiable via published iterator");
      }
    }
  }
}
