package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults.Status;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentCompactorImplTest {

  @InjectMocks
  SegmentCompactorImpl segmentCompactorImpl;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  SegmentFactory segmentFactory;
  @Spy
  ImmutableList<Segment> segmentsToBeCompacted = ImmutableList.of(mock(Segment.class),
      mock(Segment.class));

  @Test
  void duplicateKeyValueRemoval() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    doReturn(ImmutableSet.of("0-key", "key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key", "key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    // Act
    CompactionResults compactionResults = segmentCompactorImpl.compactSegments().get();

    // Assert
    assertEquals(Status.SUCCESS, compactionResults.getStatus());
    List<Segment> compactedSegments = compactionResults.getCompactedSegments();
    assertEquals(1, compactedSegments.size());
    assertEquals(createdSegment, compactedSegments.get(0));
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.getSegmentsProvidedForCompaction().toArray());
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void compactionSegmentStorageExceeded() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    doReturn(ImmutableSet.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    when(createdSegment.exceedsStorageThreshold()).thenReturn(true).thenReturn(false);

    // Act
    CompactionResults compactionResults = segmentCompactorImpl.compactSegments().get();

    // Assert
    assertEquals(Status.SUCCESS, compactionResults.getStatus());
    List<Segment> compactedSegments = compactionResults.getCompactedSegments();
    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.getSegmentsProvidedForCompaction().toArray());
  }

  @Test
  void compaction_repeatedCalls() {
    ListenableFuture<CompactionResults> firstCall = segmentCompactorImpl.compactSegments();
    assertEquals(firstCall, segmentCompactorImpl.compactSegments());
  }

  @Test
  void compactionFailure_throwsRuntimeException() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    doReturn(ImmutableSet.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.empty()).when(headSegment).read(anyString());

    // Act
    CompactionResults compactionResults = segmentCompactorImpl.compactSegments().get();

    // Assert
    assertEquals(Status.FAILED, compactionResults.getStatus());
    assertInstanceOf(RuntimeException.class, compactionResults.getFailureReason());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertNull(compactionResults.getCompactedSegments());
    assertEquals(0, compactionResults.getFailedCompactedSegments().size());
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.getSegmentsProvidedForCompaction().toArray());
  }

  @Test
  void compactionFailure_throwsIOException() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    doReturn(ImmutableSet.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key")).when(tailSegment).getSegmentKeys();
    doThrow(IOException.class).when(headSegment).read(anyString());

    Segment segment = mock(Segment.class);
    doReturn(segment).when(segmentFactory).createSegment();

    // Act
    CompactionResults compactionResults = segmentCompactorImpl.compactSegments().get();

    // Assert
    assertEquals(Status.FAILED, compactionResults.getStatus());
    assertInstanceOf(IOException.class, compactionResults.getFailureReason());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertNull(compactionResults.getCompactedSegments());
    assertEquals(1, compactionResults.getFailedCompactedSegments().size());
    assertEquals(segment, compactionResults.getFailedCompactedSegments().get(0));
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.getSegmentsProvidedForCompaction().toArray());
  }

}
