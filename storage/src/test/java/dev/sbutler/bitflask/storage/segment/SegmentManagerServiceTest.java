package dev.sbutler.bitflask.storage.segment;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentManagerServiceTest {

  SegmentManagerService segmentManagerService;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  SegmentFactory segmentFactory;
  @Mock
  SegmentCompactorFactory segmentCompactorFactory;
  @Mock
  SegmentDeleterFactory segmentDeleterFactory;
  @Mock
  SegmentLoader segmentLoader;
  @Mock
  StorageConfiguration storageConfiguration = new StorageConfiguration();
  @Mock
  ManagedSegments managedSegments;

  @BeforeEach
  void beforeEach() {
    doReturn(3).when(storageConfiguration).getStorageCompactionThreshold();
    segmentManagerService = new SegmentManagerService(
        executorService,
        segmentFactory,
        segmentCompactorFactory,
        segmentDeleterFactory,
        segmentLoader,
        storageConfiguration
    );
  }

  @Test
  void doStart() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of()).when(managedSegments).frozenSegments();
    // Act
    segmentManagerService.startAsync().awaitRunning();
  }

  @Test
  void doStart_exception() throws Exception {
    // Arrange
    doThrow(IOException.class).when(segmentLoader).loadExistingSegments();
    // Act / Assert
    assertThrows(IllegalStateException.class,
        () -> segmentManagerService.startAsync().awaitRunning());
  }

  @Test
  void doStop() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    Segment frozenSegment = mock(Segment.class);
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    // Act
    segmentManagerService.startAsync().awaitRunning();
    segmentManagerService.stopAsync().awaitTerminated();
    // Assert
    verify(writableSegment, times(1)).close();
    verify(frozenSegment, times(1)).close();
  }

  @Test
  void getManagedSegments() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of()).when(managedSegments).frozenSegments();
    // Act
    segmentManagerService.startAsync().awaitRunning();
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertEquals(managedSegments, retrievedManagedSegments);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    Segment frozenSegment = mock(Segment.class);
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertEquals(newWritableSegment, retrievedManagedSegments.writableSegment());
    assertEquals(writableSegment, retrievedManagedSegments.frozenSegments().get(0));
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_outdatedUpdate() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of()).when(managedSegments).frozenSegments();
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(mock(Segment.class));
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertEquals(writableSegment, retrievedManagedSegments.writableSegment());
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_failedSubmission() throws Exception {
    try (MockedStatic<Futures> futuresMockedStatic = mockStatic(Futures.class)) {
      // Arrange
      doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
      Segment writableSegment = mock(Segment.class);
      doReturn(writableSegment).when(managedSegments).writableSegment();
      Segment frozenSegment = mock(Segment.class);
      doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
      ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
      RejectedExecutionException e = new RejectedExecutionException();
      futuresMockedStatic.when(() -> Futures.submit(any(Runnable.class), any()))
          .thenThrow(e);
      // Act
      segmentManagerService.startAsync().awaitRunning();
      verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
      Consumer<Segment> limitConsumer = captor.getValue();
      limitConsumer.accept(writableSegment);
      // Assert
      assertEquals(e, segmentManagerService.failureCause());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_failedNewWritableCreation() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of()).when(managedSegments).frozenSegments();
    IOException e = new IOException();
    doThrow(e).when(segmentFactory).createSegment();
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    // Assert
    assertFalse(segmentManagerService.isRunning());
    assertEquals(e, segmentManagerService.failureCause());
  }

  /*@SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_initiateCompaction() throws Exception {
    // Arrange
    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    Segment frozenSegment = mock(Segment.class);
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManager.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    ManagedSegments retrievedManagedSegments = segmentManager.getManagedSegments();
    // Assert
    assertEquals(newWritableSegment, retrievedManagedSegments.writableSegment());
    assertEquals(writableSegment, retrievedManagedSegments.frozenSegments().get(0));
  }*/

  Segment compactionInitiateMocks(Segment writableSegment, ImmutableList<Segment> frozenSegments)
      throws Exception {
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(frozenSegments).when(managedSegments).frozenSegments();
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    return newWritableSegment;
  }

  SegmentCompactor compactorMock(CompactionResults compactionResults) {
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    doReturn(immediateFuture(compactionResults)).when(segmentCompactor).compactSegments();
    return segmentCompactor;
  }

  SegmentDeleter deleterMock(DeletionResults deletionResults) {
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());
    doReturn(immediateFuture(deletionResults)).when(segmentDeleter).deleteSegments();
    return segmentDeleter;
  }

  void compactionMockForDeletion() throws Exception {
    Segment writableSegment = mock(Segment.class);
    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
        mock(Segment.class));
    compactionInitiateMocks(writableSegment, frozenSegments);
    Segment compactedSegment = mock(Segment.class);
    ImmutableList<Segment> providedSegments = ImmutableList.of(writableSegment,
        frozenSegments.get(0), frozenSegments.get(1));
    CompactionResults success = new CompactionResults.Success(
        providedSegments, ImmutableList.of(compactedSegment));
    compactorMock(success);
  }
}
