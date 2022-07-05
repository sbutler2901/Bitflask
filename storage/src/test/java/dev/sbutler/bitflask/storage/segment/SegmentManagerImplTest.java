package dev.sbutler.bitflask.storage.segment;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

  Segment compactionInitiateMocks(Segment writableSegment, ImmutableList<Segment> frozenSegments)
      throws Exception {
    doReturn(writableSegment).when(managedSegments).getWritableSegment();
    doReturn(frozenSegments).when(managedSegments).getFrozenSegments();
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

  @Test
  void write_compaction_success() throws Exception {
    // Arrange
    /// Activate compaction initiation
    Segment writableSegment = mock(Segment.class);
    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
        mock(Segment.class));
    Segment newWritableSegment = compactionInitiateMocks(writableSegment, frozenSegments);
    doReturn(false).when(newWritableSegment).containsKey("key");
    /// Enable compaction mocking
    CompactionResults compactionResults = mock(CompactionResults.class);
    Segment compactedSegment = mock(Segment.class);
    doReturn(true).when(compactedSegment).containsKey("key");
    doReturn(Optional.of("value")).when(compactedSegment).read("key");
    doReturn(CompactionResults.Status.SUCCESS).when(compactionResults).getStatus();
    doReturn(ImmutableList.of(compactedSegment)).when(compactionResults)
        .getCompactedSegments();
    doReturn(ImmutableList.of(writableSegment, frozenSegments.get(0), frozenSegments.get(1)))
        .when(compactionResults).getSegmentsProvidedForCompaction();
    SegmentCompactor segmentCompactor = compactorMock(compactionResults);
    /// Enable Deletion mocking
    DeletionResults deletionResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.SUCCESS).when(deletionResults).getStatus();
    doReturn(ImmutableList.of()).when(deletionResults).getSegmentsProvidedForDeletion();
    SegmentDeleter segmentDeleter = deleterMock(deletionResults);

    // Act
    /// Activate compaction
    segmentManager.write("key", "value");
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
    Segment writableSegment = mock(Segment.class);
    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
        mock(Segment.class));
    Segment newWritableSegment = compactionInitiateMocks(writableSegment, frozenSegments);
    /// Enable compaction mocking
    CompactionResults compactionResults = mock(CompactionResults.class);
    doReturn(CompactionResults.Status.FAILED).when(compactionResults).getStatus();
    doReturn(new IOException("Compaction Failed")).when(compactionResults).getFailureReason();
    doReturn(ImmutableList.of()).when(compactionResults).getFailedCompactedSegments();
    SegmentCompactor segmentCompactor = compactorMock(compactionResults);

    // Act
    /// Activate compaction
    segmentManager.write("key", "value");
    /// Verify post update changes
    segmentManager.write("key", "value");

    // Assert
    verify(newWritableSegment, times(1)).write("key", "value");
    verify(segmentCompactor, times(2)).compactSegments();
    verify(segmentDeleterFactory, times(0)).create(any());
  }

  @Test
  void write_compaction_unexpectedFailure() throws Exception {
    // Arrange
    /// Activate compaction initiation
    Segment writableSegment = mock(Segment.class);
    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
        mock(Segment.class));
    Segment newWritableSegment = compactionInitiateMocks(writableSegment, frozenSegments);
    /// Enable compaction mocking for unexpected failure
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    doReturn(immediateFailedFuture(new Exception("unexpected"))).when(segmentCompactor)
        .compactSegments();

    // Act
    /// Activate compaction
    segmentManager.write("key", "value");
    /// Verify post update changes
    segmentManager.write("key", "value");

    // Assert
    verify(segmentCompactor, times(1)).compactSegments();
    verify(segmentDeleterFactory, times(0)).create(any());
  }

  @SuppressWarnings("ThrowableNotThrown")
  @Test
  void write_deletion_failedGeneral() throws Exception {
    // Arrange
    /// Activate compaction initiation
    Segment writableSegment = mock(Segment.class);
    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
        mock(Segment.class));
    compactionInitiateMocks(writableSegment, frozenSegments);
    /// Enable compaction mocking
    CompactionResults compactionResults = mock(
        CompactionResults.class);
    Segment compactedSegment = mock(Segment.class);
    doReturn(CompactionResults.Status.SUCCESS).when(compactionResults).getStatus();
    doReturn(ImmutableList.of(compactedSegment)).when(compactionResults)
        .getCompactedSegments();
    doReturn(ImmutableList.of(writableSegment, frozenSegments.get(0), frozenSegments.get(1)))
        .when(compactionResults).getSegmentsProvidedForCompaction();
    compactorMock(compactionResults);
    /// Mock Deleter for general failure
    DeletionResults generalFailureResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.FAILED_GENERAL).when(generalFailureResults).getStatus();
    doReturn(ImmutableList.of(mock(Segment.class), mock(Segment.class)))
        .when(generalFailureResults).getSegmentsProvidedForDeletion();
    Throwable throwable = mock(Throwable.class);
    doReturn("generalFailure").when(throwable).getMessage();
    doReturn(throwable).when(generalFailureResults).getGeneralFailureReason();
    SegmentDeleter segmentDeleter = deleterMock(generalFailureResults);

    // Act
    segmentManager.write("key", "value");

    // Assert
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  void compactionMockForDeletion() throws Exception {
    Segment writableSegment = mock(Segment.class);
    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
        mock(Segment.class));
    compactionInitiateMocks(writableSegment, frozenSegments);
    CompactionResults compactionResults = mock(
        CompactionResults.class);
    Segment compactedSegment = mock(Segment.class);
    doReturn(CompactionResults.Status.SUCCESS).when(compactionResults).getStatus();
    doReturn(ImmutableList.of(compactedSegment)).when(compactionResults)
        .getCompactedSegments();
    doReturn(ImmutableList.of(writableSegment, frozenSegments.get(0), frozenSegments.get(1)))
        .when(compactionResults).getSegmentsProvidedForCompaction();
    compactorMock(compactionResults);
  }

  @Test
  void write_deletion_failedSegment() throws Exception {
    // Arrange
    /// Enable compaction mocking
    compactionMockForDeletion();
    /// Mock Deleter for segment failure
    DeletionResults segmentFailureResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.FAILED_SEGMENTS).when(segmentFailureResults).getStatus();
    doReturn(ImmutableMap.of(mock(Segment.class), new IOException("deletion0 failed"),
        mock(Segment.class),
        new InterruptedException("deletion1 failed"))).when(segmentFailureResults)
        .getSegmentsFailureReasonsMap();
    SegmentDeleter segmentDeleter = deleterMock(segmentFailureResults);

    // Act
    segmentManager.write("key", "value");

    // Assert
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  void write_deletion_unexpectedFailure() throws Exception {
    // Arrange
    /// Enable compaction mocking
    compactionMockForDeletion();
    /// Mock Deleter for unexpected failure
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());
    doReturn(immediateFailedFuture(new Exception("unexpected"))).when(segmentDeleter)
        .deleteSegments();

    // Act
    segmentManager.write("key", "value");

    // Assert
    verify(segmentDeleter, times(1)).deleteSegments();
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
