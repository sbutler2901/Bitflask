package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

public class SegmentManagerImplTest {

  SegmentManagerImpl segmentManager;
  SegmentFactory segmentFactory;
  SegmentLoader segmentLoader;
  Provider<SegmentCompactor> segmentCompactorProvider;
  Segment activeSegment;
  Segment frozenSegment;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void beforeEach_mocks() {
    segmentFactory = mock(SegmentFactory.class);
    segmentLoader = mock(SegmentLoader.class);
    segmentCompactorProvider = mock(Provider.class);
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
        segmentCompactorProvider);
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
        segmentCompactorProvider);
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
        segmentCompactorProvider);
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
        segmentCompactorProvider);
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
        segmentCompactorProvider);
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
    doReturn(segmentCompactor).when(segmentCompactorProvider).get();
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
    doReturn(segmentCompactor).when(segmentCompactorProvider).get();
    // Setup for after compaction update
    String key = "key", value = "value";
    Segment compactedSegment = mock(Segment.class);
    doReturn(false).when(newActiveSegment).containsKey(key);
    doReturn(true).when(compactedSegment).containsKey(key);
    doReturn(Optional.of(value)).when(compactedSegment).read(key);

    ArgumentCaptor<Consumer<List<Segment>>> consumerArgumentCaptor = ArgumentCaptor.forClass(
        Consumer.class);
    // Act
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionResultsConsumer(consumerArgumentCaptor.capture());
    Consumer<List<Segment>> compactionResultsConsumer = consumerArgumentCaptor.getValue();
    compactionResultsConsumer.accept(List.of(compactedSegment));
    /// Verify post update changes
    segmentManager.read(key);

    // Assert
    verify(compactedSegment, times(1)).containsKey(key);
    verify(compactedSegment, times(1)).read(key);
  }

  @Test
  void write_compaction_completed() throws IOException {
    // Arrange
    /// Activate compaction initiation
    beforeEach_defaultFunctionality(List.of(activeSegment, frozenSegment, mock(Segment.class)));
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    Segment newActiveSegment = mock(Segment.class);
    doReturn(newActiveSegment).when(segmentFactory).createSegment();
    /// Enable compaction mocking
    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
    doReturn(segmentCompactor).when(segmentCompactorProvider).get();
    // Setup for after compaction update
    String key = "key", value = "value";
    ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    // Act
    /// Initiate compaction
    segmentManager.write(key, value);
    /// Active compaction results function
    verify(segmentCompactor).registerCompactionCompletedRunnable(runnableArgumentCaptor.capture());
    Runnable compactionCompletedRunnable = runnableArgumentCaptor.getValue();
    compactionCompletedRunnable.run();
    segmentManager.write(key, value);
    // Assert
    verify(newActiveSegment, times(1)).write(key, value);
    verify(segmentCompactor, times(2)).compactSegments();
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
    doReturn(segmentCompactor).when(segmentCompactorProvider).get();
    // Setup for after compaction update
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
