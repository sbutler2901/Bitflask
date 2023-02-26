package dev.sbutler.bitflask.storage.segmentV1;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.segmentV1.Encoder.Header;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.CompactionResults.Failed;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.CompactionResults.Success;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.Factory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentCompactorTest {

  SegmentCompactor segmentCompactor;
  @Mock
  SegmentFactory segmentFactory;
  @Spy
  ImmutableList<Segment> segmentsToBeCompacted = ImmutableList.of(mock(Segment.class),
      mock(Segment.class));

  @BeforeEach
  void setup() {
    SegmentCompactor.Factory segmentCompactorFactory = new Factory(segmentFactory,
        Thread.ofVirtual().factory());
    segmentCompactor = segmentCompactorFactory.create(segmentsToBeCompacted);
  }

  @Test
  void duplicateKeyValueRemoval() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    ImmutableMap<String, Header> headKeyHeaderMap = ImmutableMap.of(
        "key",
        Header.KEY_VALUE,
        "0-key",
        Header.KEY_VALUE
    );
    ImmutableMap<String, Header> tailKeyHeaderMap = ImmutableMap.of(
        "key",
        Header.KEY_VALUE,
        "1-key",
        Header.KEY_VALUE
    );
    doReturn(headKeyHeaderMap).when(headSegment).getSegmentKeyHeaderMap();
    doReturn(tailKeyHeaderMap).when(tailSegment).getSegmentKeyHeaderMap();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    // Act
    CompactionResults compactionResults = segmentCompactor.compactSegments();

    // Assert
    assertInstanceOf(CompactionResults.Success.class, compactionResults);
    Success success = (Success) compactionResults;
    List<Segment> compactedSegments = success.compactedSegments();
    assertEquals(1, compactedSegments.size());
    assertEquals(createdSegment, compactedSegments.get(0));
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(1)).write("key", "0-value");
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        success.segmentsProvidedForCompaction().toArray());
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void ignoreDeletedKeysAfterFirstEncounter() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    ImmutableMap<String, Header> headKeyHeaderMap = ImmutableMap.of(
        "key",
        Header.DELETED,
        "0-key",
        Header.KEY_VALUE
    );
    ImmutableMap<String, Header> tailKeyHeaderMap = ImmutableMap.of(
        "key",
        Header.KEY_VALUE,
        "1-key",
        Header.KEY_VALUE
    );
    doReturn(headKeyHeaderMap).when(headSegment).getSegmentKeyHeaderMap();
    doReturn(tailKeyHeaderMap).when(tailSegment).getSegmentKeyHeaderMap();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("0-value")).when(headSegment).read("key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    doReturn(false).when(createdSegment).exceedsStorageThreshold();

    // Act
    CompactionResults compactionResults = segmentCompactor.compactSegments();

    // Assert
    assertInstanceOf(CompactionResults.Success.class, compactionResults);
    Success success = (Success) compactionResults;
    List<Segment> compactedSegments = success.compactedSegments();
    assertEquals(1, compactedSegments.size());
    assertEquals(createdSegment, compactedSegments.get(0));
    verify(createdSegment, times(1)).write("0-key", "0-value");
    verify(createdSegment, times(1)).write("1-key", "1-value");
    verify(createdSegment, times(0)).write(eq("key"), anyString());
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        success.segmentsProvidedForCompaction().toArray());
    verify(headSegment, times(1)).markCompacted();
    verify(tailSegment, times(1)).markCompacted();
  }

  @Test
  void compactionSegmentStorageExceeded() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    ImmutableMap<String, Header> headKeyHeaderMap = ImmutableMap.of(
        "0-key",
        Header.KEY_VALUE
    );
    ImmutableMap<String, Header> tailKeyHeaderMap = ImmutableMap.of(
        "1-key",
        Header.KEY_VALUE
    );
    doReturn(headKeyHeaderMap).when(headSegment).getSegmentKeyHeaderMap();
    doReturn(tailKeyHeaderMap).when(tailSegment).getSegmentKeyHeaderMap();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    when(createdSegment.exceedsStorageThreshold()).thenReturn(true).thenReturn(false);

    // Act
    CompactionResults compactionResults = segmentCompactor.compactSegments();

    // Assert
    assertInstanceOf(CompactionResults.Success.class, compactionResults);
    Success success = (Success) compactionResults;
    List<Segment> compactedSegments = success.compactedSegments();
    assertEquals(2, compactedSegments.size());
    verify(segmentFactory, times(2)).createSegment();
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        success.segmentsProvidedForCompaction().toArray());
  }

  @Test
  void compaction_repeatedCalls() {
    // Arrange
    // Artificially cause failure to halt processing
    Segment headSegment = segmentsToBeCompacted.get(0);
    doThrow(RuntimeException.class).when(headSegment).getSegmentKeyHeaderMap();
    assertThrows(RuntimeException.class, () -> segmentCompactor.compactSegments());
    // Act
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> segmentCompactor.compactSegments());
    // Assert
    assertTrue(e.getMessage().contains("already been started"));
  }

  @Test
  void keyValueMap_readValueEmpty_throwsRuntimeException() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    ImmutableMap<String, Header> headKeyHeaderMap = ImmutableMap.of(
        "0-key",
        Header.KEY_VALUE
    );
    ImmutableMap<String, Header> tailKeyHeaderMap = ImmutableMap.of(
        "1-key",
        Header.KEY_VALUE
    );
    doReturn(headKeyHeaderMap).when(headSegment).getSegmentKeyHeaderMap();
    doReturn(tailKeyHeaderMap).when(tailSegment).getSegmentKeyHeaderMap();
    doReturn(Optional.empty()).when(headSegment).read(anyString());

    // Act
    CompactionResults compactionResults = segmentCompactor.compactSegments();

    // Assert
    assertInstanceOf(CompactionResults.Failed.class, compactionResults);
    Failed failed = (Failed) compactionResults;
    assertInstanceOf(ExecutionException.class, failed.failureReason());
    assertInstanceOf(RuntimeException.class, failed.failureReason().getCause());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertEquals(0, ((Failed) compactionResults).failedCompactionSegments().size());
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        failed.segmentsProvidedForCompaction().toArray());
  }

  @Test
  void keyValueMap_readFailure_throwsIOException() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    ImmutableMap<String, Header> headKeyHeaderMap = ImmutableMap.of(
        "0-key",
        Header.KEY_VALUE
    );
    ImmutableMap<String, Header> tailKeyHeaderMap = ImmutableMap.of(
        "1-key",
        Header.KEY_VALUE
    );
    doReturn(headKeyHeaderMap).when(headSegment).getSegmentKeyHeaderMap();
    doReturn(tailKeyHeaderMap).when(tailSegment).getSegmentKeyHeaderMap();
    doThrow(IOException.class).when(headSegment).read(anyString());

    // Act
    CompactionResults compactionResults = segmentCompactor.compactSegments();

    // Assert
    assertInstanceOf(CompactionResults.Failed.class, compactionResults);
    Failed failed = (Failed) compactionResults;
    assertInstanceOf(ExecutionException.class, failed.failureReason());
    assertInstanceOf(IOException.class, failed.failureReason().getCause());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertEquals(0, failed.failedCompactionSegments().size());
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        failed.segmentsProvidedForCompaction().toArray());
  }

  @Test
  void compaction_writeFailure_throwsIOException() throws Exception {
    // Arrange
    Segment headSegment = segmentsToBeCompacted.get(0);
    Segment tailSegment = segmentsToBeCompacted.get(1);
    ImmutableMap<String, Header> headKeyHeaderMap = ImmutableMap.of(
        "0-key",
        Header.KEY_VALUE
    );
    ImmutableMap<String, Header> tailKeyHeaderMap = ImmutableMap.of(
        "1-key",
        Header.KEY_VALUE
    );
    doReturn(headKeyHeaderMap).when(headSegment).getSegmentKeyHeaderMap();
    doReturn(tailKeyHeaderMap).when(tailSegment).getSegmentKeyHeaderMap();
    doReturn(Optional.of("0-value")).when(headSegment).read("0-key");
    doReturn(Optional.of("1-value")).when(tailSegment).read("1-key");

    Segment segment = mock(Segment.class);
    doReturn(segment).when(segmentFactory).createSegment();
    doThrow(IOException.class).when(segment).write(anyString(), anyString());

    // Act
    CompactionResults compactionResults = segmentCompactor.compactSegments();

    // Assert
    assertInstanceOf(CompactionResults.Failed.class, compactionResults);
    Failed failed = (Failed) compactionResults;
    assertInstanceOf(IOException.class, failed.failureReason());
    assertFalse(headSegment.hasBeenCompacted());
    assertFalse(tailSegment.hasBeenCompacted());
    assertEquals(1, failed.failedCompactionSegments().size());
    assertEquals(segment, failed.failedCompactionSegments().get(0));
    assertArrayEquals(List.of(headSegment, tailSegment).toArray(),
        failed.segmentsProvidedForCompaction().toArray());
  }
}
