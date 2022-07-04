package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentManagerImplTest {

  @InjectMocks
  SegmentManagerImpl segmentManager;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  SegmentFactory segmentFactory;
  @Mock
  ManagedSegments managedSegments;
  @Mock
  SegmentCompactorFactory segmentCompactorFactory;
  @Mock
  SegmentDeleterFactory segmentDeleterFactory;

  @Test
  void read_writableSegment_keyFound() throws Exception {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);
    doReturn(false).when(writableSegment).containsKey(key);
    doReturn(true).when(frozenSegment).containsKey(key);
    doReturn(valueOptional).when(frozenSegment).read(key);
    // Act
    Optional<String> readValueOptional = segmentManager.read(key);
    // Assert
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_frozenSegments_keyFound() throws Exception {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);
    doReturn(true).when(writableSegment).containsKey(key);
    doReturn(valueOptional).when(writableSegment).read(key);
    // Act
    Optional<String> readValueOptional = segmentManager.read(key);
    // Assert
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_keyNotFound() throws Exception {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
    String key = "key";
    // Act
    Optional<String> valueOptional = segmentManager.read(key);
    // Assert
    assertTrue(valueOptional.isEmpty());
  }

  @Test
  void write() throws Exception {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
    String key = "key", value = "value";
    doReturn(false).when(writableSegment).exceedsStorageThreshold();
    // Act
    segmentManager.write(key, value);
    // Assert
    verify(writableSegment, times(1)).write(key, value);
    verify(segmentFactory, times(0)).createSegment();
  }

  @Test
  void write_createNewWritableSegment() throws Exception {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
    String key = "key", value = "value";
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    // Act
    segmentManager.write(key, value);
    // Assert
    verify(writableSegment, times(1)).write(key, value);
    verify(segmentFactory, times(1)).createSegment();
  }

  Segment compactionInitiateMocks() throws Exception {
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(ImmutableList.of(frozenSegment, mock(Segment.class))).when(managedSegments)
        .getFrozenSegments();
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    return newWritableSegment;
  }

  @Test
  void write_compaction_initiate() throws Exception {
    // Arrange
    compactionInitiateMocks();
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    // Act
    segmentManager.write("key", "value");
    // Assert
    verify(segmentCompactor, times(1)).compactSegments();
  }

  SegmentCompactor compactorMock() {
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    return segmentCompactor;
  }

  CompactionResults compactionResultsMock(Segment compactedSegment) {
    CompactionResults compactionResults = mock(
        CompactionResults.class);
    doReturn(CompactionResults.Status.SUCCESS).when(compactionResults).getStatus();
    doReturn(ImmutableList.of(compactedSegment)).when(compactionResults)
        .getCompactedSegments();
    return compactionResults;
  }

  SegmentDeleter deleterMock() {
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());
    return segmentDeleter;
  }

  @SuppressWarnings("unchecked")
  Consumer<CompactionResults> compactionActiveMock(SegmentCompactor segmentCompactor)
      throws Exception {
    ArgumentCaptor<Consumer<CompactionResults>> consumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    /// Initiate compaction
    segmentManager.write("key", "value");
    /// Activate compaction results function
    verify(segmentCompactor).registerCompactionResultsConsumer(consumerArgumentCaptor.capture());
    return consumerArgumentCaptor.getValue();
  }

  @Test
  void write_compaction_updateAfter() throws Exception {
    // Arrange
    /// Activate compaction initiation
    Segment newWritableSegment = compactionInitiateMocks();
    doReturn(false).when(newWritableSegment).containsKey("key");
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = compactorMock();
    // Setup for after compaction update
    Segment compactedSegment = mock(Segment.class);
    CompactionResults compactionResults = compactionResultsMock(compactedSegment);
    doReturn(true).when(compactedSegment).containsKey("key");
    doReturn(Optional.of("value")).when(compactedSegment).read("key");
    /// Enable Deletion mocking
    SegmentDeleter segmentDeleter = deleterMock();
    DeletionResults deletionResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.SUCCESS).when(deletionResults).getStatus();
    doReturn(ImmutableList.of()).when(deletionResults).getSegmentsProvidedForDeletion();
    ListenableFuture<DeletionResults> deletionFuture = Futures.immediateFuture(deletionResults);
    doReturn(deletionFuture).when(segmentDeleter).deleteSegments();
    // Activate compaction
    Consumer<CompactionResults> compactionResultsConsumer = compactionActiveMock(segmentCompactor);

    // Act
    compactionResultsConsumer.accept(compactionResults);
    /// Verify post update changes
    segmentManager.read("key");
    segmentManager.write("key", "value");

    // Assert
    verify(segmentCompactor, times(1)).compactSegments();
    verify(compactedSegment, times(1)).containsKey("key");
    verify(compactedSegment, times(1)).read("key");
    verify(newWritableSegment, times(1)).write("key", "value");
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  @SuppressWarnings("ThrowableNotThrown")
  void write_compaction_failed() throws Exception {
    // Arrange
    /// Activate compaction initiation
    Segment newWritableSegment = compactionInitiateMocks();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = compactorMock();
    /// Enable Deletion mocking
    SegmentDeleter segmentDeleter = deleterMock();
    DeletionResults deletionResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.SUCCESS).when(deletionResults).getStatus();
    doReturn(ImmutableList.of()).when(deletionResults).getSegmentsProvidedForDeletion();
    ListenableFuture<DeletionResults> deletionFuture = Futures.immediateFuture(deletionResults);
    doReturn(deletionFuture).when(segmentDeleter).deleteSegments();
    // Activate compaction
    Consumer<CompactionResults> compactionResultsConsumer = compactionActiveMock(segmentCompactor);

    CompactionResults compactionResults = mock(CompactionResults.class);
    doReturn(CompactionResults.Status.FAILED).when(compactionResults).getStatus();
    doReturn(new Throwable("Compaction Failed")).when(compactionResults).getFailureReason();
    doReturn(ImmutableList.of()).when(compactionResults).getFailedCompactedSegments();

    // Act
    compactionResultsConsumer.accept(compactionResults);
    segmentManager.write("key", "value");

    // Assert
    verify(newWritableSegment, times(1)).write("key", "value");
    verify(segmentCompactor, times(2)).compactSegments();
    verify(segmentDeleterFactory, times(1)).create(any());
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
  void write_deletion() throws Exception {
    // Arrange
    /// Activate compaction initiation
    compactionInitiateMocks();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = compactorMock();
    /// Enable Deletion mocking
    deleterMock();
    /// Setup for after compaction update
    Segment compactedSegment = mock(Segment.class);
    CompactionResults compactionResults = compactionResultsMock(compactedSegment);
    /// Deletion results
    Segment deletion0 = mock(Segment.class);
    Segment deletion1 = mock(Segment.class);

    // Act
    FutureCallback<DeletionResults> deletionCallback;
    try (MockedStatic<Futures> futuresMockedStatic = mockStatic(Futures.class)) {
      /// Activate compaction
      Consumer<CompactionResults> compactionResultsConsumer = compactionActiveMock(
          segmentCompactor);
      compactionResultsConsumer.accept(compactionResults);
      ArgumentCaptor<FutureCallback<DeletionResults>> deletionCallbackCaptor = ArgumentCaptor.forClass(
          FutureCallback.class);

      futuresMockedStatic.verify(
          () -> Futures.addCallback(any(), deletionCallbackCaptor.capture(), any()));
      deletionCallback = deletionCallbackCaptor.getValue();
    }

    // Act - Success
    DeletionResults successResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.SUCCESS).when(successResults).getStatus();
    doReturn(ImmutableList.of(deletion0, deletion1)).when(successResults)
        .getSegmentsProvidedForDeletion();
    deletionCallback.onSuccess(successResults);

    // Act - General Failure
    DeletionResults generalFailureResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.FAILED_GENERAL).when(generalFailureResults).getStatus();
    doReturn(ImmutableList.of(deletion0, deletion1)).when(generalFailureResults)
        .getSegmentsProvidedForDeletion();
    Throwable throwable = mock(Throwable.class);
    doReturn("generalFailure").when(throwable).getMessage();
    doReturn(throwable).when(generalFailureResults).getGeneralFailureReason();
    deletionCallback.onSuccess(generalFailureResults);

    // Act - Segment Failure
    DeletionResults segmentFailureResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.FAILED_SEGMENTS).when(segmentFailureResults).getStatus();
    doReturn(ImmutableMap.of(deletion0, new IOException("deletion0 failed"), deletion1,
        new InterruptedException("deletion1 failed"))).when(segmentFailureResults)
        .getSegmentsFailureReasonsMap();
    deletionCallback.onSuccess(segmentFailureResults);

    // Act - Unexpected Error
    RejectedExecutionException exception = new
        RejectedExecutionException("test unexpected failure");
    deletionCallback.onFailure(exception);

    // Assert
    // todo: creation assertions
  }

  @Test
  void close() {
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
    segmentManager.close();
    verify(writableSegment, times(1)).close();
    verify(frozenSegment, times(1)).close();
  }
}
