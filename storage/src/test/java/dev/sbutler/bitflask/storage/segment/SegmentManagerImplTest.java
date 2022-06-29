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
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SegmentManagerImplTest {

  SegmentManagerImpl segmentManager;
  SegmentFactory segmentFactory;
  SegmentLoader segmentLoader;
  SegmentCompactorFactory segmentCompactorFactory;
  SegmentDeleterFactory segmentDeleterFactory;
  Segment activeSegment;
  Segment frozenSegment;

  @BeforeEach
  void beforeEach_mocks() {
    segmentFactory = mock(SegmentFactory.class);
    segmentLoader = mock(SegmentLoader.class);
    segmentCompactorFactory = mock(SegmentCompactorFactory.class);
    segmentDeleterFactory = mock(SegmentDeleterFactory.class);
    activeSegment = mock(Segment.class);
    frozenSegment = mock(Segment.class);
  }

  @Test
  void initialize_dirStoreCreated() throws IOException {
    // Arrange
    doReturn(true).when(segmentFactory).createSegmentStoreDir();
    doReturn(activeSegment).when(segmentFactory).createSegment();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader, segmentCompactorFactory,
        segmentDeleterFactory);
    // Assert
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_NoSegmentsLoaded() throws IOException {
    // Arrange
    ImmutableList<Segment> mockSegments = mock(ImmutableList.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(true).when(mockSegments).isEmpty();
    doReturn(activeSegment).when(segmentFactory).createSegment();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader, segmentCompactorFactory,
        segmentDeleterFactory);
    // Assert
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_SegmentsLoaded_headExceedsThreshold() throws IOException {
    // Arrange
    ImmutableList<Segment> mockSegments = mock(ImmutableList.class);
    Segment segment = mock(Segment.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegments).isEmpty();
    doReturn(segment).when(mockSegments).get(0);
    doReturn(true).when(segment).exceedsStorageThreshold();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader, segmentCompactorFactory,
        segmentDeleterFactory);
    // Assert
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_SegmentsLoaded_headBelowThreshold() throws IOException {
    // Arrange
    ImmutableList<Segment> mockSegments = mock(ImmutableList.class);
    Segment segment = mock(Segment.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegments).isEmpty();
    doReturn(segment).when(mockSegments).get(0);
    doReturn(false).when(segment).exceedsStorageThreshold();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader, segmentCompactorFactory,
        segmentDeleterFactory);
    // Assert
    verify(segmentFactory, times(0)).createSegment();
  }

  void beforeEach_defaultFunctionality() throws IOException {
    beforeEach_defaultFunctionality(ImmutableList.of(activeSegment, frozenSegment));
  }

  void beforeEach_defaultFunctionality(ImmutableList<Segment> mockLoadedSegments)
      throws IOException {
    doReturn(mockLoadedSegments).when(segmentLoader).loadExistingSegments();
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader, segmentCompactorFactory,
        segmentDeleterFactory);
  }

  @Test
  void read_activeSegment_keyFound() throws IOException {
    // Arrange
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);
    doReturn(false).when(activeSegment).containsKey(key);
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
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);
    doReturn(true).when(activeSegment).containsKey(key);
    doReturn(valueOptional).when(activeSegment).read(key);
    // Act
    Optional<String> readValueOptional = segmentManager.read(key);
    // Assert
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_keyNotFound() throws IOException {
    // Arrange
    beforeEach_defaultFunctionality();
    String key = "key";
    // Act
    Optional<String> valueOptional = segmentManager.read(key);
    // Assert
    assertTrue(valueOptional.isEmpty());
  }

  @Test
  void write() throws IOException {
    // Arrange
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    doReturn(false).when(activeSegment).exceedsStorageThreshold();
    // Act
    segmentManager.write(key, value);
    // Assert
    verify(activeSegment, times(1)).write(key, value);
    verify(segmentFactory, times(0)).createSegment();
  }

  @Test
  void write_createNewActiveSegment() throws IOException {
    // Arrange
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    // Act
    segmentManager.write(key, value);
    // Assert
    verify(activeSegment, times(1)).write(key, value);
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  void write_compaction_initiate() throws IOException {
    // Arrange
    beforeEach_defaultFunctionality(
        ImmutableList.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
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
    beforeEach_defaultFunctionality(
        ImmutableList.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    Segment newActiveSegment = mock(Segment.class);
    doReturn(newActiveSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    // Setup for after compaction update
    String key = "key", value = "value";
    Segment compactedSegment = mock(Segment.class);
    doReturn(false).when(newActiveSegment).containsKey(key);
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
    verify(newActiveSegment, times(1)).write(key, value);
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
  void write_compaction_failed() throws IOException {
    // Arrange
    /// Activate compaction initiation
    beforeEach_defaultFunctionality(
        ImmutableList.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    Segment newActiveSegment = mock(Segment.class);
    doReturn(newActiveSegment).when(segmentFactory).createSegment();
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
    verify(newActiveSegment, times(1)).write(key, value);
    verify(segmentCompactor, times(2)).compactSegments();
    verify(segmentDeleterFactory, times(1)).create(any());
    verify(segmentDeleter, times(1)).deleteSegments();
  }

  @Test
  @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
  void write_deletion() throws IOException {
    // Arrange
    /// Activate compaction initiation
    beforeEach_defaultFunctionality(
        ImmutableList.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    Segment newActiveSegment = mock(Segment.class);
    doReturn(newActiveSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
    // Setup for after compaction update
    String key = "key", value = "value";
    Segment compactedSegment = mock(Segment.class);
    doReturn(false).when(newActiveSegment).containsKey(key);
    doReturn(true).when(compactedSegment).containsKey(key);
    doReturn(Optional.of(value)).when(compactedSegment).read(key);
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
    doReturn(ImmutableList.of(deletion0, deletion1)).when(segmentFailureResults)
        .getSegmentsProvidedForDeletion();
    doReturn(ImmutableMap.of(deletion0, new IOException("deletion0 failed"), deletion1,
        new InterruptedException("deletion1 failed"))).when(segmentFailureResults)
        .getSegmentsFailureReasonsMap();
    deletionResultsConsumer.accept(segmentFailureResults);

    // Assert
    // todo: creation assertions
  }

  @Test
  void close() throws IOException {
    beforeEach_defaultFunctionality();
    segmentManager.close();
    verify(activeSegment, times(1)).close();
    verify(frozenSegment, times(1)).close();
  }
}
