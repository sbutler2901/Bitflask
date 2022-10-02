package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageThreadFactory;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.FailedGeneral;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.FailedSegments;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.Success;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.Factory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentDeleterTest {

  SegmentDeleter segmentDeleter;
  @Spy
  StorageThreadFactory storageThreadFactory = new StorageThreadFactory();
  @Spy
  ImmutableList<Segment> segmentsToBeCompacted = ImmutableList.of(mock(Segment.class),
      mock(Segment.class));

  @BeforeEach
  void setup() {
    SegmentDeleter.Factory segmentDeleterFactory = new Factory(storageThreadFactory);
    segmentDeleter = segmentDeleterFactory.create(segmentsToBeCompacted);
  }

  @Test
  void deletion_success() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    // Act
    DeletionResults deletionResults = segmentDeleter.deleteSegments();
    // Assert
    assertInstanceOf(Success.class, deletionResults);
    Success success = (Success) deletionResults;
    assertEquals(headSegment, success.segmentsProvidedForDeletion().get(0));
    assertEquals(tailSegment, success.segmentsProvidedForDeletion().get(1));
    verify(headSegment, times(1)).close();
    verify(headSegment, times(1)).deleteSegment();
    verify(tailSegment, times(1)).close();
    verify(tailSegment, times(1)).deleteSegment();
  }

  @Test
  void deletion_repeatedCalls() {
    // Arrange
    SegmentDeleter.Factory segmentDeleterFactory = new Factory(storageThreadFactory);
    segmentDeleter = segmentDeleterFactory.create(ImmutableList.of());
    segmentDeleter.deleteSegments();
    // Act
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> segmentDeleter.deleteSegments());
    // Assert
    assertTrue(e.getMessage().contains("already been started"));
  }

  @Test
  void deletion_closeAndDelete_Exception() {
    // Arrange
    SegmentDeleter.Factory segmentDeleterFactory = new Factory(storageThreadFactory);
    // Artificially cause failure
    segmentDeleter = segmentDeleterFactory.create(null);
    // Act
    DeletionResults deletionResults = segmentDeleter.deleteSegments();
    // Assert
    assertInstanceOf(FailedGeneral.class, deletionResults);
    FailedGeneral failedGeneral = (FailedGeneral) deletionResults;
    assertNull(failedGeneral.segmentsProvidedForDeletion());
  }

  @Test
  void deletion_segmentFailures() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    doThrow(IOException.class).when(headSegment).deleteSegment();
    // Act
    DeletionResults deletionResults = segmentDeleter.deleteSegments();
    // Assert
    assertInstanceOf(FailedSegments.class, deletionResults);
    FailedSegments failedSegments = (FailedSegments) deletionResults;
    Map<Segment, Throwable> segmentFailureReasonMap = failedSegments.segmentsFailureReasonsMap();
    assertEquals(1, segmentFailureReasonMap.size());
    assertInstanceOf(ExecutionException.class, segmentFailureReasonMap.get(headSegment));
  }
}
