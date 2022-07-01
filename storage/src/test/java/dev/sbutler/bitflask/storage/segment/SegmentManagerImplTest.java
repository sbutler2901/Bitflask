package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import dev.sbutler.bitflask.storage.segment.SegmentManagerImpl.ManagedSegments;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentManagerImplTest {

  @InjectMocks
  SegmentManagerImpl segmentManager;
  @Mock
  SegmentFactory segmentFactory;
  @Mock
  ManagedSegments managedSegments;
  @Mock
  SegmentCompactorFactory segmentCompactorFactory;
  @Mock
  SegmentDeleterFactory segmentDeleterFactory;

  @Test
  void read_writableSegment_keyFound() throws IOException {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
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
  void read_frozenSegments_keyFound() throws IOException {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
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
  void read_keyNotFound() throws IOException {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    String key = "key";
    // Act
    Optional<String> valueOptional = segmentManager.read(key);
    // Assert
    assertTrue(valueOptional.isEmpty());
  }

  @Test
  void write() throws IOException {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    String key = "key", value = "value";
    doReturn(false).when(writableSegment).exceedsStorageThreshold();
    // Act
    segmentManager.write(key, value);
    // Assert
    verify(writableSegment, times(1)).write(key, value);
    verify(segmentFactory, times(0)).createSegment();
  }

  @Test
  void write_createNewWritableSegment() throws IOException {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    String key = "key", value = "value";
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    // Act
    segmentManager.write(key, value);
    // Assert
    verify(writableSegment, times(1)).write(key, value);
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  void write_compaction_initiate() throws IOException {
    // Arrange
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment, mock(Segment.class))).when(managedSegments)
        .frozenSegments();
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    // Act
    segmentManager.write("key", "value");
    // Assert
    verify(segmentCompactor, times(1)).compactSegments();
  }

  @Test
  @SuppressWarnings("unchecked")
  void write_compaction_updateAfter() throws IOException {
    // Arrange
    /// Activate compaction initiation
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment, mock(Segment.class))).when(managedSegments)
        .frozenSegments();
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    // Setup for after compaction update
    String key = "key", value = "value";
    Segment compactedSegment = mock(Segment.class);
    doReturn(false).when(newWritableSegment).containsKey(key);
    doReturn(true).when(compactedSegment).containsKey(key);
    doReturn(Optional.of(value)).when(compactedSegment).read(key);
    CompactionResults compactionResults = mock(
        CompactionResults.class);
    doReturn(CompactionResults.Status.SUCCESS).when(compactionResults).getStatus();
    doReturn(ImmutableList.of(compactedSegment)).when(compactionResults)
        .getCompactedSegments();
    /// Enable Deletion mocking
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());

    ArgumentCaptor<Consumer<CompactionResults>> consumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionResultsConsumer(consumerArgumentCaptor.capture());
    Consumer<CompactionResults> compactionResultsConsumer = consumerArgumentCaptor.getValue();

    // Act
    compactionResultsConsumer.accept(compactionResults);
    /// Verify post update changes
    segmentManager.read(key);
    segmentManager.write(key, value);

    // Assert
    verify(segmentCompactor, times(1)).compactSegments();
    verify(compactedSegment, times(1)).containsKey(key);
    verify(compactedSegment, times(1)).read(key);
    verify(newWritableSegment, times(1)).write(key, value);
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
  void write_compaction_failed() throws IOException {
    // Arrange
    /// Activate compaction initiation
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment, mock(Segment.class))).when(managedSegments)
        .frozenSegments();
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    /// Setup for after compaction update
    String key = "key", value = "value";
    ArgumentCaptor<Consumer<CompactionResults>> consumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    /// Deletion of failed compaction segments
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionResultsConsumer(consumerArgumentCaptor.capture());
    Consumer<CompactionResults> compactionResultsConsumer = consumerArgumentCaptor.getValue();

    CompactionResults compactionResults = mock(CompactionResults.class);
    doReturn(CompactionResults.Status.FAILED).when(compactionResults).getStatus();
    doReturn(new Throwable("Compaction Failed")).when(compactionResults).getFailureReason();
    doReturn(ImmutableList.of()).when(compactionResults).getFailedCompactedSegments();

    // Act
    compactionResultsConsumer.accept(compactionResults);
    segmentManager.write(key, value);
    // Assert
    verify(newWritableSegment, times(1)).write(key, value);
    verify(segmentCompactor, times(2)).compactSegments();
    verify(segmentDeleterFactory, times(1)).create(any());
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
  void write_deletion() throws IOException {
    // Arrange
    /// Activate compaction initiation
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment, mock(Segment.class))).when(managedSegments)
        .frozenSegments();
    doReturn(true).when(writableSegment).exceedsStorageThreshold();
    Segment newWritableSegment = mock(Segment.class);
    doReturn(newWritableSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    // Setup for after compaction update
    String key = "key", value = "value";
    Segment compactedSegment = mock(Segment.class);
    CompactionResults compactionResults = mock(
        CompactionResults.class);
    doReturn(ImmutableList.of(compactedSegment)).when(compactionResults)
        .getCompactedSegments();
    /// Enable Deletion mocking
    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());

    ArgumentCaptor<Consumer<CompactionResults>> compactionConsumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionResultsConsumer(
        compactionConsumerArgumentCaptor.capture());
    Consumer<CompactionResults> compactionResultsConsumer = compactionConsumerArgumentCaptor.getValue();
    doReturn(CompactionResults.Status.SUCCESS).when(compactionResults).getStatus();
    compactionResultsConsumer.accept(compactionResults);

    /// Deletion results
    Segment deletion0 = mock(Segment.class);
    Segment deletion1 = mock(Segment.class);
    doReturn(0).when(deletion0).getSegmentFileKey();
    doReturn(1).when(deletion1).getSegmentFileKey();

    /// Capture Deletion
    ArgumentCaptor<Consumer<DeletionResults>> deletionConsumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    verify(segmentDeleter).registerDeletionResultsConsumer(
        deletionConsumerArgumentCaptor.capture());
    Consumer<DeletionResults> deletionResultsConsumer = deletionConsumerArgumentCaptor.getValue();

    // Act - Success
    DeletionResults successResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.SUCCESS).when(successResults).getStatus();
    doReturn(ImmutableList.of(deletion0, deletion1)).when(successResults)
        .getSegmentsProvidedForDeletion();
    deletionResultsConsumer.accept(successResults);

    // Act - General Failure
    DeletionResults generalFailureResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.FAILED_GENERAL).when(generalFailureResults).getStatus();
    doReturn(ImmutableList.of(deletion0, deletion1)).when(generalFailureResults)
        .getSegmentsProvidedForDeletion();
    Throwable throwable = mock(Throwable.class);
    doReturn("generalFailure").when(throwable).getMessage();
    doReturn(throwable).when(generalFailureResults).getGeneralFailureReason();
    deletionResultsConsumer.accept(generalFailureResults);

    // Act - Segment Failure
    DeletionResults segmentFailureResults = mock(DeletionResults.class);
    doReturn(DeletionResults.Status.FAILED_SEGMENTS).when(segmentFailureResults).getStatus();
    doReturn(ImmutableMap.of(deletion0, new IOException("deletion0 failed"), deletion1,
        new InterruptedException("deletion1 failed"))).when(segmentFailureResults)
        .getSegmentsFailureReasonsMap();
    deletionResultsConsumer.accept(segmentFailureResults);

    // Assert
    // todo: creation assertions
  }

  @Test
  void close() {
    Segment writableSegment = mock(Segment.class);
    Segment frozenSegment = mock(Segment.class);
    doReturn(writableSegment).when(managedSegments).writableSegment();
    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).frozenSegments();
    segmentManager.close();
    verify(writableSegment, times(1)).close();
    verify(frozenSegment, times(1)).close();
  }
}
