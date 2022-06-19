package dev.sbutler.bitflask.storage.segment.compactor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.segment.Segment;
import dev.sbutler.bitflask.storage.segment.SegmentFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentCompactorImplTest {

  SegmentCompactorImpl segmentCompactorImpl;
  ExecutorService executorService;
  SegmentFactory segmentFactory;
  List<Segment> preCompactedSegmentsList;

  @BeforeEach
  void beforeEach() {
    executorService = mock(ExecutorService.class);
    segmentFactory = mock(SegmentFactory.class);
    preCompactedSegmentsList = List.of(mock(Segment.class), mock(Segment.class));
    segmentCompactorImpl = new SegmentCompactorImpl(executorService, segmentFactory);
    segmentCompactorImpl.setPreCompactedSegments(new ArrayList<>(preCompactedSegmentsList));
  }

  @Test
  void duplicateKeyValueRemoval() throws IOException {
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

    // todo: update
//    List<Segment> compactedSegments = segmentCompactorImpl.compactSegments();

//    assertEquals(1, compactedSegments.size());
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    verify(createdSegment, times(0)).write("key", "1-value");
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void compactionFailure_throwsRuntimeException() throws IOException {
    Segment headSegment = preCompactedSegmentsList.get(0);
    Segment tailSegment = preCompactedSegmentsList.get(1);

    doReturn(Set.of("0-key")).when(headSegment).getSegmentKeys();
    doReturn(Set.of("1-key")).when(tailSegment).getSegmentKeys();
    doReturn(Optional.empty()).when(headSegment).read(anyString());
    doReturn(Optional.empty()).when(tailSegment).read(anyString());

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();

    assertThrows(RuntimeException.class, () -> segmentCompactorImpl.compactSegments());
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

    // todo: update
//    List<Segment> compactedSegments = segmentCompactorImpl.compactSegments();

//    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
  }

//  @Test
//  void closeAndDeleteSegments() throws IOException {
//    segmentCompactorImpl.closeAndDeleteSegments();
//    verify(preCompactedSegmentsList.get(0), times(1)).closeAndDelete();
//    verify(preCompactedSegmentsList.get(1), times(1)).closeAndDelete();
//  }
//
//  @Test
//  void closeAndDeleteSegments_IOException() throws IOException {
//    Segment headSegment = preCompactedSegmentsList.get(0);
//    doThrow(IOException.class).when(headSegment).closeAndDelete();
//    List<Segment> failedSegments = segmentCompactorImpl.closeAndDeleteSegments();
//    assertEquals(1, failedSegments.size());
//    assertEquals(headSegment, failedSegments.get(0));
//  }

}
