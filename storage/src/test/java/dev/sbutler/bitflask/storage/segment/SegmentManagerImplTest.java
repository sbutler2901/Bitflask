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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class SegmentManagerImplTest {

  SegmentManagerImpl segmentManager;
  SegmentFactory segmentFactory;
  SegmentLoader segmentLoader;
  Segment segment;

  @BeforeEach
  void beforeEach() throws IOException {
    segmentFactory = mock(SegmentFactory.class);
    segmentLoader = mock(SegmentLoader.class);
    segment = mock(Segment.class);

    Deque<Segment> mockLoadedSegments = new ArrayDeque<>(List.of(segment));
    doReturn(mockLoadedSegments).when(segmentLoader).loadExistingSegments();

    segmentManager = new SegmentManagerImpl(segmentFactory, segmentLoader);
    segmentManager.logger = mock(Logger.class);
  }

  @Test
  void write() throws IOException {
    doReturn(false).when(segment).exceedsStorageThreshold();
    String key = "key", value = "value";
    segmentManager.write(key, value);
    verify(segment, times(1)).write(key, value);
  }

  @Test
  void read_keyFound() throws IOException {
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);

    doReturn(true).when(segment).containsKey(key);
    doReturn(valueOptional).when(segment).read(key);

    Optional<String> readValueOptional = segmentManager.read(key);
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_keyNotFound() throws IOException {
    String key = "key";
    Optional<String> valueOptional = segmentManager.read(key);
    assertTrue(valueOptional.isEmpty());
  }

}
