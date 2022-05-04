package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SegmentManagerImplTest {

  SegmentFactory segmentFactory = mock(SegmentFactory.class);

  @Test
  void write() throws IOException {
    Segment segment = mock(Segment.class);
    doReturn(false).when(segment).exceedsStorageThreshold();
    doReturn(segment).when(segmentFactory).createSegment();

    SegmentManagerImpl segmentManager = new SegmentManagerImpl(segmentFactory);

    String key = "key", value = "value";
    segmentManager.write(key, value);
    verify(segment, times(1)).write(key, value);
  }

  @Test
  void read_keyFound() throws IOException {
    Segment segment = mock(Segment.class);
    doReturn(false).when(segment).exceedsStorageThreshold();
    doReturn(segment).when(segmentFactory).createSegment();

    SegmentManagerImpl segmentManager = new SegmentManagerImpl(segmentFactory);
    String key = "key", value = "value";
    Optional<String> valueOptional = Optional.of(value);
    doReturn(true).when(segment).containsKey(key);
    doReturn(valueOptional).when(segment).read(key);

    Optional<String> readValueOptional = segmentManager.read(key);
    assertEquals(valueOptional, readValueOptional);
  }

  @Test
  void read_keyNotFound() throws IOException {
    Segment segment = mock(Segment.class);
    doReturn(false).when(segment).exceedsStorageThreshold();
    doReturn(segment).when(segmentFactory).createSegment();

    SegmentManagerImpl segmentManager = new SegmentManagerImpl(segmentFactory);
    String key = "key";
    Optional<String> valueOptional = segmentManager.read(key);
    assertTrue(valueOptional.isEmpty());
  }

}
