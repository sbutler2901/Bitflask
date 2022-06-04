package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class SegmentManagerImplTest {

  SegmentManagerImpl segmentManager;
  ExecutorService executorService;
  SegmentFactory segmentFactory;
  SegmentLoader segmentLoader;
  Segment segment;

  @BeforeEach
  void beforeEach_mocks() {
    executorService = mock(ExecutorService.class);
    segmentFactory = mock(SegmentFactory.class);
    segmentLoader = mock(SegmentLoader.class);
    segment = mock(Segment.class);
    SegmentManagerImpl.logger = mock(Logger.class);
  }

  @Test
  void initializeSegmentsDeque_storeCreated() throws IOException {
    doReturn(true).when(segmentFactory).createSegmentStoreDir();
    doReturn(segment).when(segmentFactory).createSegment();

    segmentManager = new SegmentManagerImpl(executorService, segmentFactory, segmentLoader);

    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initializeSegmentsDeque_storeExisted_NoSegmentsLoaded() throws IOException {
    Deque<Segment> mockSegmentDeque = mock(Deque.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegmentDeque).when(segmentLoader).loadExistingSegments();
    doReturn(true).when(mockSegmentDeque).isEmpty();
    doReturn(segment).when(segmentFactory).createSegment();

    segmentManager = new SegmentManagerImpl(executorService, segmentFactory, segmentLoader);

    verify(segmentFactory, times(1)).createSegment();
  }

  @Test
  @SuppressWarnings("unchecked")
  void initializeSegmentsDeque_storeExisted_SegmentsLoaded() throws IOException {
    Deque<Segment> mockSegmentDeque = mock(Deque.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    doReturn(mockSegmentDeque).when(segmentLoader).loadExistingSegments();
    doReturn(false).when(mockSegmentDeque).isEmpty();

    segmentManager = new SegmentManagerImpl(executorService, segmentFactory, segmentLoader);

    verify(segmentFactory, times(0)).createSegment();
  }

  void beforeEach_defaultFunctionality() throws IOException {
    Deque<Segment> mockLoadedSegments = new ArrayDeque<>(List.of(segment));
    doReturn(mockLoadedSegments).when(segmentLoader).loadExistingSegments();
    segmentManager = new SegmentManagerImpl(executorService, segmentFactory, segmentLoader);
  }

  @Test
  void write() throws IOException {
    beforeEach_defaultFunctionality();
    doReturn(false).when(segment).exceedsStorageThreshold();
    String key = "key", value = "value";
    segmentManager.write(key, value);
    verify(segment, times(1)).write(key, value);
  }

  @Test
  void read_keyFound() throws IOException {
    beforeEach_defaultFunctionality();
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);

    doReturn(true).when(segment).containsKey(key);
    doReturn(valueOptional).when(segment).read(key);

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

}
