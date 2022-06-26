package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionCompletionResults;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        List.of(headSegment, tailSegment));
    doAnswer((InvocationOnMock invocation) -> {
      ((Runnable) invocation.getArguments()[0]).run();
      return null;
    }).when(executorService).execute(any(Runnable.class));
  }

  @Test
  void duplicateKeyValueRemoval() throws Exception {
    // Arrange
    doReturn(Set.of("0-key", "key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key", "key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    AtomicReference<CompactionCompletionResults> compactionCompletionResults = new AtomicReference<>();
    segmentCompactorImpl.registerCompactionCompletedConsumer(compactionCompletionResults::set);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    List<Segment> compactedSegments = compactionCompletionResults.get().compactedSegments();
    assertEquals(1, compactedSegments.size());
    assertEquals(createdSegment, compactedSegments.get(0));
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void compactionSegmentStorageExceeded() throws Exception {
    // Arrange
    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    when(createdSegment.exceedsStorageThreshold()).thenReturn(true).thenReturn(false);

    AtomicReference<CompactionCompletionResults> compactionCompletionResults = new AtomicReference<>();
    segmentCompactorImpl.registerCompactionCompletedConsumer(compactionCompletionResults::set);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    List<Segment> compactedSegments = compactionCompletionResults.get().compactedSegments();
    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
  }

  @Test
  void compactionFailure_throwsRuntimeException() throws IOException {
    // Arrange
    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.empty()).when(headSegment).read(anyString());

    AtomicReference<Throwable> handledException = new AtomicReference<>();
    List<Segment> failedCompactionSegments = new ArrayList<>();
    segmentCompactorImpl.registerCompactionFailedConsumer((throwable, failedCompactionSegment) -> {
      handledException.set(throwable);
      failedCompactionSegments.addAll(failedCompactionSegment);
    });

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertInstanceOf(RuntimeException.class, handledException.get());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertEquals(0, failedCompactionSegments.size());
  }

  @Test
  void compactionFailure_throwsIOException() throws IOException {
    // Arrange
    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doThrow(IOException.class).when(headSegment).read(anyString());

    Segment segment = mock(Segment.class);
    doReturn(segment).when(segmentFactory).createSegment();

    AtomicReference<Throwable> handledException = new AtomicReference<>();
    List<Segment> failedCompactionSegments = new ArrayList<>();
    segmentCompactorImpl.registerCompactionFailedConsumer((throwable, failedCompactionSegment) -> {
      handledException.set(throwable);
      failedCompactionSegments.addAll(failedCompactionSegment);
    });

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertInstanceOf(IOException.class, handledException.get());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertEquals(1, failedCompactionSegments.size());
    assertEquals(segment, failedCompactionSegments.get(0));
  }

}
