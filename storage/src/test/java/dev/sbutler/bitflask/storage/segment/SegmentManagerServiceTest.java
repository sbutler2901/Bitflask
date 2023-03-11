package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
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
  SegmentCompactor.Factory segmentCompactorFactory;
  @Mock
  SegmentDeleter.Factory segmentDeleterFactory;
  @Mock
  SegmentLoader segmentLoader;
  @Mock
  StorageConfigurations storageConfigurations = new StorageConfigurations();
  @Mock
  ManagedSegments managedSegments;

  @BeforeEach
  void beforeEach() {
    when(storageConfigurations.getStorageCompactionThreshold()).thenReturn(3);
    segmentManagerService = new SegmentManagerService(
        executorService,
        segmentFactory,
        segmentCompactorFactory,
        segmentDeleterFactory,
        segmentLoader,
        storageConfigurations
    );
  }

  @Test
  void doStart() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    // Act
    segmentManagerService.startAsync().awaitRunning();
  }

  @Test
  void doStart_exception() {
    // Arrange
    SegmentLoaderException segmentLoaderException = new SegmentLoaderException("test");
    doThrow(segmentLoaderException).when(segmentLoader).loadExistingSegments();
    // Act
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> segmentManagerService.startAsync().awaitRunning());
    // Assert
    assertThat(e).hasCauseThat().isEqualTo(segmentLoaderException);
  }

  @Test
  void doStop() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    Segment frozenSegment = mock(Segment.class);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of(frozenSegment));
    // Act
    segmentManagerService.startAsync().awaitRunning();
    segmentManagerService.stopAsync().awaitTerminated();
    // Assert
    verify(writableSegment, times(1)).close();
    verify(frozenSegment, times(1)).close();
  }

  @Test
  void doStop_exception() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());

    RuntimeException runtimeException = new RuntimeException("test");
    doThrow(runtimeException).when(writableSegment).close();
    // Act
    segmentManagerService.startAsync().awaitRunning();

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> segmentManagerService.stopAsync().awaitTerminated());
    // Assert
    assertThat(e).hasCauseThat().isEqualTo(runtimeException);
  }

  @Test
  void getManagedSegments() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    // Act
    segmentManagerService.startAsync().awaitRunning();
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertThat(retrievedManagedSegments).isEqualTo(managedSegments);
  }

  @Test
  void getWritableSegment() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    // Act
    segmentManagerService.startAsync().awaitRunning();
    WritableSegment segment = segmentManagerService.getWritableSegment();
    // Assert
    assertThat(segment).isEqualTo(writableSegment);
  }

  @Test
  void getReadableSegments() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of(frozenSegment));
    // Act
    segmentManagerService.startAsync().awaitRunning();
    ImmutableList<ReadableSegment> readableSegments = segmentManagerService.getReadableSegments();
    // Assert
    assertThat(readableSegments).hasSize(2);
    assertThat(readableSegments).containsExactly(writableSegment, frozenSegment).inOrder();
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer() throws Exception {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    Segment frozenSegment = mock(Segment.class);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of(frozenSegment));
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of(frozenSegment));
    Segment newWritableSegment = mock(Segment.class);
    when(segmentFactory.createSegment()).thenReturn(newWritableSegment);
    when(segmentFactory.createSegment()).thenReturn(newWritableSegment);
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertThat(retrievedManagedSegments.writableSegment()).isEqualTo(newWritableSegment);
    assertThat(retrievedManagedSegments.frozenSegments().get(0)).isEqualTo(writableSegment);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_outdatedUpdate() {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(mock(Segment.class));
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertThat(retrievedManagedSegments.writableSegment()).isEqualTo(writableSegment);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_failedSubmission() {
    try (MockedStatic<Futures> futuresMockedStatic = mockStatic(Futures.class)) {
      // Arrange
      when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
      Segment writableSegment = mock(Segment.class);
      when(managedSegments.writableSegment()).thenReturn(writableSegment);
      Segment frozenSegment = mock(Segment.class);
      when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of(frozenSegment));
      ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
      RejectedExecutionException e = new RejectedExecutionException();
      futuresMockedStatic.when(() -> Futures.submit(any(Runnable.class), any()))
          .thenCallRealMethod()
          .thenThrow(e);
      // Act
      segmentManagerService.startAsync().awaitRunning();
      verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
      Consumer<Segment> limitConsumer = captor.getValue();
      limitConsumer.accept(writableSegment);
      // Assert
      assertThat(segmentManagerService.isRunning()).isFalse();
      assertThat(segmentManagerService.failureCause()).isEqualTo(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_failedNewWritableCreation() throws Exception {
    // Arrange
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    IOException e = new IOException();
    doThrow(e).when(segmentFactory).createSegment();
    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    // Assert
    assertThat(segmentManagerService.isRunning()).isFalse();
    assertThat(segmentManagerService.failureCause()).isEqualTo(e);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_compactionSuccess() throws Exception {
    // Arrange
    setupForCompaction();

    // Callback initializing compaction
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    Segment newWritableSegment = mock(Segment.class);
    when(segmentFactory.createSegment()).thenReturn(newWritableSegment);

    // After compaction
    when(writableSegment.hasBeenCompacted()).thenReturn(true);
    ImmutableList<Segment> segmentsForCompaction = ImmutableList.of(writableSegment);
    Segment compactedSegment = mock(Segment.class);
    CompactionResults compactionResults = new CompactionResults.Success(
        segmentsForCompaction,
        ImmutableList.of(compactedSegment));
    compactorMock(compactionResults);

    // After deletion
    DeletionResults deletionResults =
        new SegmentDeleter.DeletionResults.Success(segmentsForCompaction);
    deleterMock(deletionResults);

    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertThat(retrievedManagedSegments.writableSegment()).isEqualTo(newWritableSegment);
    assertThat(retrievedManagedSegments.frozenSegments()).hasSize(1);
    assertThat(retrievedManagedSegments.frozenSegments()).containsExactly(compactedSegment);
    verify(segmentDeleterFactory, times(1)).create(segmentsForCompaction);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_compactionFailure() throws Exception {
    // Arrange
    setupForCompaction();

    // Callback initializing compaction
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    Segment newWritableSegment = mock(Segment.class);
    when(segmentFactory.createSegment()).thenReturn(newWritableSegment);

    // After compaction
    ImmutableList<Segment> segmentsForCompaction = ImmutableList.of(writableSegment);
    ImmutableList<Segment> failedCompactionSegments = ImmutableList.of(mock(Segment.class));
    CompactionResults compactionResults = new CompactionResults.Failed(
        segmentsForCompaction,
        new IOException("test"),
        failedCompactionSegments);
    compactorMock(compactionResults);

    // After deletion
    DeletionResults deletionResults =
        new SegmentDeleter.DeletionResults.Success(segmentsForCompaction);
    deleterMock(deletionResults);

    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    ManagedSegments retrievedManagedSegments = segmentManagerService.getManagedSegments();
    // Assert
    assertThat(retrievedManagedSegments.writableSegment()).isEqualTo(newWritableSegment);
    assertThat(retrievedManagedSegments.frozenSegments()).hasSize(1);
    assertThat(retrievedManagedSegments.frozenSegments()).containsExactly(writableSegment);
    verify(segmentDeleterFactory, times(1)).create(failedCompactionSegments);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_deletionFailedGeneral() throws Exception {
    // Arrange
    setupForCompaction();

    // Callback initializing compaction
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    Segment newWritableSegment = mock(Segment.class);
    when(segmentFactory.createSegment()).thenReturn(newWritableSegment);

    // After compaction
    when(writableSegment.hasBeenCompacted()).thenReturn(true);
    ImmutableList<Segment> segmentsForCompaction = ImmutableList.of(writableSegment);
    Segment compactedSegment = mock(Segment.class);
    CompactionResults compactionResults = new CompactionResults.Success(
        segmentsForCompaction,
        ImmutableList.of(compactedSegment));
    compactorMock(compactionResults);

    // After deletion
    DeletionResults deletionResults =
        new SegmentDeleter.DeletionResults.FailedGeneral(segmentsForCompaction,
            new IOException("test"));
    deleterMock(deletionResults);

    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    // Assert
    verify(segmentDeleterFactory, times(1)).create(segmentsForCompaction);
  }

  @SuppressWarnings("unchecked")
  @Test
  void segmentSizeLimitExceededConsumer_deletionFailedSegments() throws Exception {
    // Arrange
    setupForCompaction();

    // Callback initializing compaction
    when(segmentLoader.loadExistingSegments()).thenReturn(managedSegments);
    Segment writableSegment = mock(Segment.class);
    when(managedSegments.writableSegment()).thenReturn(writableSegment);
    when(managedSegments.frozenSegments()).thenReturn(ImmutableList.of());
    Segment newWritableSegment = mock(Segment.class);
    when(segmentFactory.createSegment()).thenReturn(newWritableSegment);

    // After compaction
    when(writableSegment.hasBeenCompacted()).thenReturn(true);
    ImmutableList<Segment> segmentsForCompaction = ImmutableList.of(writableSegment);
    Segment compactedSegment = mock(Segment.class);
    CompactionResults compactionResults = new CompactionResults.Success(
        segmentsForCompaction,
        ImmutableList.of(compactedSegment));
    compactorMock(compactionResults);

    // After deletion
    DeletionResults deletionResults =
        new SegmentDeleter.DeletionResults.FailedSegments(segmentsForCompaction,
            ImmutableMap.of(segmentsForCompaction.get(0), new IOException("test")));
    deleterMock(deletionResults);

    ArgumentCaptor<Consumer<Segment>> captor = ArgumentCaptor.forClass(Consumer.class);
    // Act
    segmentManagerService.startAsync().awaitRunning();
    verify(writableSegment).registerSizeLimitExceededConsumer(captor.capture());
    Consumer<Segment> limitConsumer = captor.getValue();
    limitConsumer.accept(writableSegment);
    // Assert
    verify(segmentDeleterFactory, times(1)).create(segmentsForCompaction);
  }

  private void setupForCompaction() {
    when(storageConfigurations.getStorageCompactionThreshold()).thenReturn(1);
    segmentManagerService = new SegmentManagerService(
        executorService,
        segmentFactory,
        segmentCompactorFactory,
        segmentDeleterFactory,
        segmentLoader,
        storageConfigurations
    );
  }

  private void compactorMock(CompactionResults compactionResults) {
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    when(segmentCompactorFactory.create(any())).thenReturn(segmentCompactor);
    when(segmentCompactor.compactSegments()).thenReturn(compactionResults);
  }

  private void deleterMock(DeletionResults deletionResults) {
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    when(segmentDeleterFactory.create(any())).thenReturn(segmentDeleter);
    when(segmentDeleter.deleteSegments()).thenReturn(deletionResults);
  }
}
