package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionCompletionResults;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

public class SegmentManagerImplTest {

  SegmentManagerImpl segmentManager;
  SegmentFactory segmentFactory;
  SegmentLoader segmentLoader;
  SegmentCompactorFactory segmentCompactorFactory;
  Segment activeSegment;
  Segment frozenSegment;

  @BeforeEach
  void beforeEach_mocks() {
    segmentFactory = mock(SegmentFactory.class);
    segmentLoader = mock(SegmentLoader.class);
    segmentCompactorFactory = mock(SegmentCompactorFactory.class);
    activeSegment = mock(Segment.class);
    frozenSegment = mock(Segment.class);
    SegmentManagerImpl.logger = mock(Logger.class);
  }

  @Test
  void initialize_dirStoreCreated() throws IOException {
    // Arrange
    doReturn(true).when(segmentFactory).createSegmentStoreDir();
    doReturn(activeSegment).when(segmentFactory).createSegment();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorFactory);
    // Assert
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_NoSegmentsLoaded() throws IOException {
    // Arrange
    List<Segment> mockSegments = mock(List.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(true).when(mockSegments).isEmpty();
    doReturn(activeSegment).when(segmentFactory).createSegment();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorFactory);
    // Assert
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_SegmentsLoaded_headExceedsThreshold() throws IOException {
    // Arrange
    List<Segment> mockSegments = mock(List.class);
    Segment segment = mock(Segment.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegments).isEmpty();
    doReturn(segment).when(mockSegments).get(0);
    doReturn(true).when(segment).exceedsStorageThreshold();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorFactory);
    // Assert
    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_SegmentsLoaded_headBelowThreshold() throws IOException {
    // Arrange
    List<Segment> mockSegments = mock(List.class);
    Segment segment = mock(Segment.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegments).isEmpty();
    doReturn(segment).when(mockSegments).get(0);
    doReturn(false).when(segment).exceedsStorageThreshold();
    // Act
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorFactory);
    // Assert
    verify(segmentFactory, times(0)).createSegment();
  }

  void beforeEach_defaultFunctionality() throws IOException {
    beforeEach_defaultFunctionality(List.of(activeSegment, frozenSegment));
  }

  void beforeEach_defaultFunctionality(List<Segment> loadedSegments) throws IOException {
    List<Segment> mockLoadedSegments = new ArrayList<>(loadedSegments);
    doReturn(mockLoadedSegments).when(segmentLoader).loadExistingSegments();
    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorFactory);
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
    beforeEach_defaultFunctionality(List.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(anyList());
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
    beforeEach_defaultFunctionality(List.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    Segment newActiveSegment = mock(Segment.class);
    doReturn(newActiveSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(anyList());
    // Setup for after compaction update
    String key = "key", value = "value";
    Segment compactedSegment = mock(Segment.class);
    doReturn(false).when(newActiveSegment).containsKey(key);
    doReturn(true).when(compactedSegment).containsKey(key);
    doReturn(Optional.of(value)).when(compactedSegment).read(key);
    CompactionCompletionResults compactionCompletionResults = mock(
        CompactionCompletionResults.class);
    doReturn(List.of(compactedSegment)).when(compactionCompletionResults).compactedSegments();

    ArgumentCaptor<Consumer<CompactionCompletionResults>> consumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    // Act
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionCompletedConsumer(consumerArgumentCaptor.capture());
    Consumer<CompactionCompletionResults> compactionResultsConsumer = consumerArgumentCaptor.getValue();
    compactionResultsConsumer.accept(compactionCompletionResults);
    /// Verify post update changes
    segmentManager.read(key);
    segmentManager.write(key, value);

    // Assert
    verify(segmentCompactor, times(1)).compactSegments();
    verify(compactedSegment, times(1)).containsKey(key);
    verify(compactedSegment, times(1)).read(key);
    verify(newActiveSegment, times(1)).write(key, value);
  }

  @Test
  @SuppressWarnings("unchecked")
  void write_compaction_failed() throws IOException {
    // Arrange
    /// Activate compaction initiation
    beforeEach_defaultFunctionality(List.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    Segment newActiveSegment = mock(Segment.class);
    doReturn(newActiveSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorFactory).create(anyList());
    /// Setup for after compaction update
    String key = "key", value = "value";
    ArgumentCaptor<Consumer<Throwable>> consumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    // Act
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionFailedConsumer(consumerArgumentCaptor.capture());
    Consumer<Throwable> compactionFailedConsumer = consumerArgumentCaptor.getValue();
    compactionFailedConsumer.accept(new Throwable("test"));
    segmentManager.write(key, value);
    // Assert
    verify(newActiveSegment, times(1)).write(key, value);
    verify(segmentCompactor, times(2)).compactSegments();
  }
}
