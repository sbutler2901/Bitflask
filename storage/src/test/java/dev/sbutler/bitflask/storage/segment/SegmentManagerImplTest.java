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
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    doReturn(true).when(segmentFactory).createSegmentStoreDir();
    doReturn(activeSegment).when(segmentFactory).createSegment();

    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorProvider);

    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_NoSegmentsLoaded() throws IOException {
    List<Segment> mockSegments = mock(List.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(true).when(mockSegments).isEmpty();
    doReturn(activeSegment).when(segmentFactory).createSegment();

    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorProvider);

    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_SegmentsLoaded_headExceedsThreshold() throws IOException {
    List<Segment> mockSegments = mock(List.class);
    Segment segment = mock(Segment.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegments).isEmpty();
    doReturn(segment).when(mockSegments).get(0);
    doReturn(true).when(segment).exceedsStorageThreshold();

    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorProvider);

    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initialize_dirStoreExisted_SegmentsLoaded_headBelowThreshold() throws IOException {
    List<Segment> mockSegments = mock(List.class);
    Segment segment = mock(Segment.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegments).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegments).isEmpty();
    doReturn(segment).when(mockSegments).get(0);
    doReturn(false).when(segment).exceedsStorageThreshold();

    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader,
        segmentCompactorProvider);

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
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);

    doReturn(false).when(activeSegment).containsKey(key);
    doReturn(true).when(frozenSegment).containsKey(key);
    doReturn(valueOptional).when(frozenSegment).read(key);

    Optional<String> readValueOptional = segmentManager.read(key);
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_frozenSegments_keyFound() throws IOException {
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);

    doReturn(true).when(activeSegment).containsKey(key);
    doReturn(valueOptional).when(activeSegment).read(key);

    Optional<String> readValueOptional = segmentManager.read(key);
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_keyNotFound() throws IOException {
    beforeEach_defaultFunctionality();
    String key = "key";
    Optional<String> valueOptional = segmentManager.read(key);
    assertTrue(valueOptional.isEmpty());
  }

  @Test
  void write() throws IOException {
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    doReturn(false).when(activeSegment).exceedsStorageThreshold();
    segmentManager.write(key, value);
    verify(activeSegment, times(1)).write(key, value);
    verify(segmentFactory, times(0)).createSegment();
  }

  @Test
  void write_createNewActiveSegment() throws IOException {
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    doReturn(true).when(activeSegment).exceedsStorageThreshold();
    segmentManager.write(key, value);
    verify(activeSegment, times(1)).write(key, value);
    verify(segmentFactory, times(1)).createSegment();
  }

//  @Test
//  void write_initiateCompaction() throws IOException {
//    beforeEach_defaultFunctionality(List.of(activeSegment, frozenSegment, mock(Segment.class)));
//
//  }
}
