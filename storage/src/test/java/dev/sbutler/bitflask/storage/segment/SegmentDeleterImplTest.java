package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.Status;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentDeleterImplTest {

  SegmentDeleterImpl segmentDeleterImpl;
  @Mock
  ExecutorService executorService;
  @Mock
  Segment headSegment;
  @Mock
  Segment tailSegment;

  @BeforeEach
  void beforeEach() {
    segmentDeleterImpl = new SegmentDeleterImpl(executorService,
        ImmutableList.of(headSegment, tailSegment));
    doAnswer((InvocationOnMock invocation) -> {
      ((Runnable) invocation.getArguments()[0]).run();
      return null;
    }).when(executorService).execute(any(Runnable.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletion_success() throws InterruptedException, IOException {
    // Arrange
    Future<Void> headFuture = mock(Future.class);
    Future<Void> tailFuture = mock(Future.class);
    doReturn(List.of(headFuture, tailFuture)).when(executorService).invokeAll(anyList());

    AtomicReference<DeletionResults> deletionResultsAtomicReference = new AtomicReference<>();
    Consumer<DeletionResults> deletionResultsConsumer = deletionResultsAtomicReference::set;
    segmentDeleterImpl.registerDeletionResultsConsumer(deletionResultsConsumer);

    ArgumentCaptor<List<Callable<Void>>> invokeAllArgumentCaptor = ArgumentCaptor.forClass(
        List.class);

    // Act
    segmentDeleterImpl.deleteSegments();
    verify(executorService).invokeAll(invokeAllArgumentCaptor.capture());
    invokeAllArgumentCaptor.getValue().forEach(voidCallable -> {
      try {
        voidCallable.call();
      } catch (Exception e) {
        fail();
      }
    });
    // Assert
    DeletionResults deletionResults = deletionResultsAtomicReference.get();
    assertEquals(Status.SUCCESS, deletionResults.getStatus());
    assertEquals(headSegment, deletionResults.getSegmentsProvidedForDeletion().get(0));
    assertEquals(tailSegment, deletionResults.getSegmentsProvidedForDeletion().get(1));
    verify(headSegment, times(1)).close();
    verify(headSegment, times(1)).delete();
    verify(tailSegment, times(1)).close();
    verify(tailSegment, times(1)).delete();
  }

  @Test
  void deletion_executorInterrupted() throws InterruptedException {
    // Arrange
    InterruptedException interruptedException = new InterruptedException("Interrupted");
    doThrow(interruptedException).when(executorService).invokeAll(anyList());
    AtomicReference<DeletionResults> deletionResultsAtomicReference = new AtomicReference<>();
    Consumer<DeletionResults> deletionResultsConsumer = deletionResultsAtomicReference::set;
    segmentDeleterImpl.registerDeletionResultsConsumer(deletionResultsConsumer);
    // Act
    segmentDeleterImpl.deleteSegments();
    // Assert
    DeletionResults deletionResults = deletionResultsAtomicReference.get();
    assertEquals(Status.FAILED_GENERAL, deletionResults.getStatus());
    assertEquals(interruptedException, deletionResults.getGeneralFailureReason());
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletion_segmentFailures() throws InterruptedException, ExecutionException {
    // Arrange
    Future<Void> headFuture = mock(Future.class);
    ExecutionException executionException = mock(ExecutionException.class);
    doReturn(new IOException("head ioexception")).when(executionException).getCause();
    doThrow(executionException).when(headFuture).get();
    Future<Void> tailFuture = mock(Future.class);
    doThrow(InterruptedException.class).when(tailFuture).get();
    doReturn(List.of(headFuture, tailFuture)).when(executorService).invokeAll(anyList());
    AtomicReference<DeletionResults> deletionResultsAtomicReference = new AtomicReference<>();
    Consumer<DeletionResults> deletionResultsConsumer = deletionResultsAtomicReference::set;
    segmentDeleterImpl.registerDeletionResultsConsumer(deletionResultsConsumer);
    // Act
    segmentDeleterImpl.deleteSegments();
    // Assert
    DeletionResults deletionResults = deletionResultsAtomicReference.get();
    assertEquals(Status.FAILED_SEGMENTS, deletionResults.getStatus());
    Map<Segment, Throwable> segmentFailureReasonMap = deletionResults.getSegmentsFailureReasonsMap();
    assertEquals(2, segmentFailureReasonMap.size());
    assertInstanceOf(IOException.class, segmentFailureReasonMap.get(headSegment));
    assertInstanceOf(InterruptedException.class, segmentFailureReasonMap.get(tailSegment));
  }
}
