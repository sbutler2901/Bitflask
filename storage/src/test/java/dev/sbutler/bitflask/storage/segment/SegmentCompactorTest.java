package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentCompactorTest {

  SegmentCompactor segmentCompactor;
  SegmentFactory segmentFactory;
  List<Segment> preCompactedSegmentsList;

  @BeforeEach
  void beforeEach() {
    segmentFactory = mock(SegmentFactory.class);
    preCompactedSegmentsList = List.of(mock(Segment.class), mock(Segment.class));
    segmentCompactor = new SegmentCompactor(segmentFactory,
        new ArrayDeque<>(preCompactedSegmentsList));
  }

  @Test
  void duplicateRemoval() throws IOException {
    Segment headSegment = preCompactedSegmentsList.get(0);
    Segment tailSegment = preCompactedSegmentsList.get(1);

    doReturn(Set.of("0-key", "key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key", "key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    Deque<Segment> compactedSegments = segmentCompactor.call();

    assertEquals(1, compactedSegments.size());
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    verify(createdSegment, times(0)).write("key", "1-value");
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void runtimeException() throws IOException {
    Segment headSegment = preCompactedSegmentsList.get(0);
    Segment tailSegment = preCompactedSegmentsList.get(1);

    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.empty()).when(headSegment).read(anyString());
    doReturn(Optional.empty()).when(tailSegment).read(anyString());

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();

    assertThrows(RuntimeException.class, () -> segmentCompactor.call());
  }

  @Test
  void compactionSegmentStorageExceeded() throws IOException {
    Segment headSegment = preCompactedSegmentsList.get(0);
    Segment tailSegment = preCompactedSegmentsList.get(1);

    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    when(createdSegment.exceedsStorageThreshold()).thenReturn(true).thenReturn(false);

    Deque<Segment> compactedSegments = segmentCompactor.call();

    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
  }

}
