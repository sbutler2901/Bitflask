package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults.Status;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentCompactorImplTest {

  SegmentCompactorImpl segmentCompactorImpl;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentFactory segmentFactory;
  @Mock
  Segment headSegment;
  @Mock
  Segment tailSegment;

  @BeforeEach
  void beforeEach() {
    segmentCompactorImpl = new SegmentCompactorImpl(executorService, segmentFactory,
        ImmutableList.of(headSegment, tailSegment));
    doAnswer((InvocationOnMock invocation) -> {
      ((Runnable) invocation.getArguments()[0]).run();
      return null;
    }).when(executorService).execute(any(Runnable.class));
  }

  @Test
  void duplicateKeyValueRemoval() throws Exception {
    // Arrange
    doReturn(ImmutableSet.of("0-key", "key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key", "key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    AtomicReference<CompactionResults> compactionResults = new AtomicReference<>();
    segmentCompactorImpl.registerCompactionResultsConsumer(compactionResults::set);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(Status.SUCCESS, compactionResults.get().getStatus());
    List<Segment> compactedSegments = compactionResults.get().getCompactedSegments();
    assertEquals(1, compactedSegments.size());
    assertEquals(createdSegment, compactedSegments.get(0));
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.get().getSegmentsProvidedForCompaction().toArray());
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void compactionSegmentStorageExceeded() throws Exception {
    // Arrange
    doReturn(ImmutableSet.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    when(createdSegment.exceedsStorageThreshold()).thenReturn(true).thenReturn(false);

    AtomicReference<CompactionResults> compactionResults = new AtomicReference<>();
    segmentCompactorImpl.registerCompactionResultsConsumer(compactionResults::set);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(Status.SUCCESS, compactionResults.get().getStatus());
    List<Segment> compactedSegments = compactionResults.get().getCompactedSegments();
    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.get().getSegmentsProvidedForCompaction().toArray());
  }

  @Test
  void compactionFailure_throwsRuntimeException() throws IOException {
    // Arrange
    doReturn(ImmutableSet.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.empty()).when(headSegment).read(anyString());

    AtomicReference<CompactionResults> compactionResults = new AtomicReference<>();
    segmentCompactorImpl.registerCompactionResultsConsumer(compactionResults::set);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(Status.FAILED, compactionResults.get().getStatus());
    assertInstanceOf(RuntimeException.class, compactionResults.get().getFailureReason());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertNull(compactionResults.get().getCompactedSegments());
    assertEquals(0, compactionResults.get().getFailedCompactedSegments().size());
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.get().getSegmentsProvidedForCompaction().toArray());
  }

  @Test
  void compactionFailure_throwsIOException() throws IOException {
    // Arrange
    doReturn(ImmutableSet.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(ImmutableSet.of("1-key")).when(tailSegment).getSegmentKeys();
    doThrow(IOException.class).when(headSegment).read(anyString());

    Segment segment = mock(Segment.class);
    doReturn(segment).when(segmentFactory).createSegment();

    AtomicReference<CompactionResults> compactionResults = new AtomicReference<>();
    segmentCompactorImpl.registerCompactionResultsConsumer(compactionResults::set);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(Status.FAILED, compactionResults.get().getStatus());
    assertInstanceOf(IOException.class, compactionResults.get().getFailureReason());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertNull(compactionResults.get().getCompactedSegments());
    assertEquals(1, compactionResults.get().getFailedCompactedSegments().size());
    assertEquals(segment, compactionResults.get().getFailedCompactedSegments().get(0));
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        compactionResults.get().getSegmentsProvidedForCompaction().toArray());
  }

}
