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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentCompactorImplTest {

  @InjectMocks
  SegmentCompactorImpl segmentCompactorImpl;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentFactory segmentFactory;

  @BeforeEach
  void beforeEach() {
    doAnswer((InvocationOnMock invocation) -> {
      ((Runnable) invocation.getArguments()[0]).run();
      return null;
    }).when(executorService).execute(any(Runnable.class));
  }

  @SuppressWarnings("unchecked")
  void mockCloseAndDeleteInvokeAll(List<Future<?>> results) throws InterruptedException {
    doAnswer((InvocationOnMock invocation) -> {
      List<Callable<?>> closeAndDeleteSegments =
          ((List<Callable<?>>) invocation.getArguments()[0]);
      closeAndDeleteSegments.forEach((callable) -> {
        try {
          callable.call();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      return results;
    }).when(executorService).invokeAll(any(List.class));
  }

  @Test
  void duplicateKeyValueRemoval() throws Exception {
    // Arrange
    Segment headSegment = mock(Segment.class);
    Segment tailSegment = mock(Segment.class);
    segmentCompactorImpl.setPreCompactedSegments(List.of(headSegment, tailSegment));

    doReturn(Set.of("0-key", "key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key", "key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    List<Segment> compactedSegments = new ArrayList<>();
    segmentCompactorImpl.registerCompactedSegmentsConsumer(compactedSegments::addAll);

    mockCloseAndDeleteInvokeAll(List.of(
        CompletableFuture.completedFuture(Void.TYPE),
        CompletableFuture.completedFuture(Void.TYPE)
    ));

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(1, compactedSegments.size());
    assertEquals(createdSegment, compactedSegments.get(0));
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
    verify(headSegment, times(1)).closeAndDelete();
    verify(tailSegment, times(1)).closeAndDelete();
  }

  @Test
  void compactionSegmentStorageExceeded() throws Exception {
    // Arrange
    Segment headSegment = mock(Segment.class);
    Segment tailSegment = mock(Segment.class);
    segmentCompactorImpl.setPreCompactedSegments(List.of(headSegment, tailSegment));

    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    when(createdSegment.exceedsStorageThreshold()).thenReturn(true).thenReturn(false);

    List<Segment> compactedSegments = new ArrayList<>();
    segmentCompactorImpl.registerCompactedSegmentsConsumer(compactedSegments::addAll);

    mockCloseAndDeleteInvokeAll(List.of(
        CompletableFuture.completedFuture(Void.TYPE),
        CompletableFuture.completedFuture(Void.TYPE)
    ));

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
  }

  @Test
  void compactionFailure_throwsRuntimeException() throws IOException {
    // Arrange
    Segment headSegment = mock(Segment.class);
    Segment tailSegment = mock(Segment.class);
    segmentCompactorImpl.setPreCompactedSegments(List.of(headSegment, tailSegment));

    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.empty()).when(headSegment).read(anyString());

    List<Throwable> handledExceptions = new ArrayList<>();
    segmentCompactorImpl.registerCompactionFailedConsumer(handledExceptions::add);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(1, handledExceptions.size());
    assertInstanceOf(RuntimeException.class, handledExceptions.get(0));
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
  }

  @Test
  void compactionFailure_throwsIOException() throws IOException {
    // Arrange
    Segment headSegment = mock(Segment.class);
    Segment tailSegment = mock(Segment.class);
    segmentCompactorImpl.setPreCompactedSegments(List.of(headSegment, tailSegment));

    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doThrow(IOException.class).when(headSegment).read(anyString());

    List<Throwable> handledExceptions = new ArrayList<>();
    segmentCompactorImpl.registerCompactionFailedConsumer(handledExceptions::add);

    // Act
    segmentCompactorImpl.compactSegments();

    // Assert
    assertEquals(1, handledExceptions.size());
    assertInstanceOf(IOException.class, handledExceptions.get(0));
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
  }

}
