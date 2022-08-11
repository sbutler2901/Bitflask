package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.FailedGeneral;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.FailedSegments;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.Success;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentDeleterImplTest {

  @InjectMocks
  SegmentDeleterImpl segmentDeleterImpl;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Spy
  ImmutableList<Segment> segmentsToBeCompacted = ImmutableList.of(mock(Segment.class),
      mock(Segment.class));

  @Test
  void deletion_success() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    // Act
    DeletionResults deletionResults = segmentDeleterImpl.deleteSegments().get();
    // Assert
    assertInstanceOf(Success.class, deletionResults);
    Success success = (Success) deletionResults;
    assertEquals(headSegment, success.segmentsProvidedForDeletion().get(0));
    assertEquals(tailSegment, success.segmentsProvidedForDeletion().get(1));
    verify(headSegment, times(1)).close();
    verify(headSegment, times(1)).delete();
    verify(tailSegment, times(1)).close();
    verify(tailSegment, times(1)).delete();
  }

  @Test
  void deletion_repeatedCalls() {
    ListenableFuture<DeletionResults> firstCall = segmentDeleterImpl.deleteSegments();
    assertEquals(firstCall, segmentDeleterImpl.deleteSegments());
  }

  @Test
  void deletion_executorInterrupted() throws Exception {
    // Arrange
    InterruptedException interruptedException = new InterruptedException("Interrupted");
    doThrow(interruptedException).when(executorService).invokeAll(anyList());
    // Act
    DeletionResults deletionResults = segmentDeleterImpl.deleteSegments().get();
    assertInstanceOf(FailedGeneral.class, deletionResults);
    FailedGeneral failedGeneral = (FailedGeneral) deletionResults;
    // Assert
    assertEquals(interruptedException, failedGeneral.failureReason());
  }

  @Test
  void deletion_segmentFailures() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    doThrow(IOException.class).when(headSegment).delete();
    // Act
    DeletionResults deletionResults = segmentDeleterImpl.deleteSegments().get();
    // Assert
    assertInstanceOf(FailedSegments.class, deletionResults);
    FailedSegments failedSegments = (FailedSegments) deletionResults;
    Map<Segment, Throwable> segmentFailureReasonMap = failedSegments.segmentsFailureReasonsMap();
    assertEquals(1, segmentFailureReasonMap.size());
    assertInstanceOf(ExecutionException.class, segmentFailureReasonMap.get(headSegment));
  }
}
