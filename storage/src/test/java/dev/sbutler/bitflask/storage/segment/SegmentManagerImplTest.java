//package dev.sbutler.bitflask.storage.segment;
//
//import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
//import static com.google.common.util.concurrent.Futures.immediateFuture;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.ImmutableMap;
//import com.google.common.util.concurrent.ListeningExecutorService;
//import com.google.common.util.concurrent.testing.TestingExecutors;
//import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
//import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
//import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
//import java.io.IOException;
//import java.util.Optional;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//public class SegmentManagerImplTest {
//
//  @InjectMocks
//  SegmentManager segmentManager;
//  @Spy
//  @SuppressWarnings("UnstableApiUsage")
//  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
//  @Mock
//  SegmentFactory segmentFactory;
//  @Mock
//  SegmentCompactorFactory segmentCompactorFactory;
//  @Mock
//  SegmentDeleterFactory segmentDeleterFactory;
//  @Mock
//  SegmentLoader segmentLoader;
//  @Mock
//  ManagedSegments managedSegments;
//
//  @BeforeEach
//  void beforeEach() throws Exception {
//    doReturn(managedSegments).when(segmentLoader).loadExistingSegments();
//    segmentManager.initialize();
//  }
//
//  @Test
//  void read_writableSegment_keyFound() throws Exception {
//    // Arrange
//    Segment writableSegment = mock(Segment.class);
//    Segment frozenSegment = mock(Segment.class);
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
//    String key = "key", value = "value";
//    Optional<String> valueOptional = Optional.of(value);
//    doReturn(false).when(writableSegment).containsKey(key);
//    doReturn(true).when(frozenSegment).containsKey(key);
//    doReturn(valueOptional).when(frozenSegment).read(key);
//    // Act
//    Optional<String> readValueOptional = segmentManager.read(key);
//    // Assert
//    assertEquals(valueOptional, readValueOptional);
//  }
//
//  @Test
//  void read_frozenSegments_keyFound() throws Exception {
//    // Arrange
//    Segment writableSegment = mock(Segment.class);
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    String key = "key", value = "value";
//    Optional<String> valueOptional = Optional.of(value);
//    doReturn(true).when(writableSegment).containsKey(key);
//    doReturn(valueOptional).when(writableSegment).read(key);
//    // Act
//    Optional<String> readValueOptional = segmentManager.read(key);
//    // Assert
//    assertEquals(valueOptional, readValueOptional);
//  }
//
//  @Test
//  void read_keyNotFound() throws Exception {
//    // Arrange
//    Segment writableSegment = mock(Segment.class);
//    Segment frozenSegment = mock(Segment.class);
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
//    String key = "key";
//    // Act
//    Optional<String> valueOptional = segmentManager.read(key);
//    // Assert
//    assertTrue(valueOptional.isEmpty());
//  }
//
//  @Test
//  void write() throws Exception {
//    // Arrange
//    Segment writableSegment = mock(Segment.class);
//    Segment frozenSegment = mock(Segment.class);
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
//    String key = "key", value = "value";
//    doReturn(false).when(writableSegment).exceedsStorageThreshold();
//    // Act
//    segmentManager.write(key, value);
//    // Assert
//    verify(writableSegment, times(1)).write(key, value);
//    verify(segmentFactory, times(0)).createSegment();
//  }
//
//  @Test
//  void write_createNewWritableSegment() throws Exception {
//    // Arrange
//    Segment writableSegment = mock(Segment.class);
//    Segment frozenSegment = mock(Segment.class);
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
//    String key = "key", value = "value";
//    doReturn(true).when(writableSegment).exceedsStorageThreshold();
//    // Act
//    segmentManager.write(key, value);
//    // Assert
//    verify(writableSegment, times(1)).write(key, value);
//    verify(segmentFactory, times(1)).createSegment();
//  }
//
//  Segment compactionInitiateMocks(Segment writableSegment, ImmutableList<Segment> frozenSegments)
//      throws Exception {
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    doReturn(frozenSegments).when(managedSegments).getFrozenSegments();
//    doReturn(true).when(writableSegment).exceedsStorageThreshold();
//    Segment newWritableSegment = mock(Segment.class);
//    doReturn(newWritableSegment).when(segmentFactory).createSegment();
//    return newWritableSegment;
//  }
//
//  SegmentCompactor compactorMock(CompactionResults compactionResults) {
//    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
//    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
//    doReturn(immediateFuture(compactionResults)).when(segmentCompactor).compactSegments();
//    return segmentCompactor;
//  }
//
//  SegmentDeleter deleterMock(DeletionResults deletionResults) {
//    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
//    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());
//    doReturn(immediateFuture(deletionResults)).when(segmentDeleter).deleteSegments();
//    return segmentDeleter;
//  }
//
//  @Test
//  void write_compaction_success() throws Exception {
//    // Arrange
//    /// Activate compaction initiation
//    Segment writableSegment = mock(Segment.class);
//    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
//        mock(Segment.class));
//    Segment newWritableSegment = compactionInitiateMocks(writableSegment, frozenSegments);
//    doReturn(false).when(newWritableSegment).containsKey("key");
//    /// Enable compaction mocking
//    Segment compactedSegment = mock(Segment.class);
//    ImmutableList<Segment> providedSegments = ImmutableList.of(writableSegment,
//        frozenSegments.get(0), frozenSegments.get(1));
//    CompactionResults success = new CompactionResults.Success(
//        providedSegments, ImmutableList.of(compactedSegment));
//    doReturn(true).when(compactedSegment).containsKey("key");
//    doReturn(Optional.of("value")).when(compactedSegment).read("key");
//    SegmentCompactor segmentCompactor = compactorMock(success);
//    /// Enable Deletion mocking
//    DeletionResults deletionResults = new DeletionResults.Success(ImmutableList.of());
//    SegmentDeleter segmentDeleter = deleterMock(deletionResults);
//
//    // Act
//    /// Activate compaction
//    segmentManager.write("key", "value");
//    /// Verify post update changes
//    segmentManager.read("key");
//    segmentManager.write("key", "value");
//
//    // Assert
//    verify(segmentCompactor, times(1)).compactSegments();
//    verify(compactedSegment, times(1)).containsKey("key");
//    verify(compactedSegment, times(1)).read("key");
//    verify(newWritableSegment, times(1)).write("key", "value");
//    verify(segmentDeleter, times(1)).deleteSegments();
//  }
//
//  @Test
//  void write_compaction_failed() throws Exception {
//    // Arrange
//    /// Activate compaction initiation
//    Segment writableSegment = mock(Segment.class);
//    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
//        mock(Segment.class));
//    Segment newWritableSegment = compactionInitiateMocks(writableSegment, frozenSegments);
//    /// Enable compaction mocking
//    CompactionResults failed = new CompactionResults.Failed(
//        ImmutableList.of(),
//        new IOException("Compaction Failed"),
//        ImmutableList.of()
//    );
//    SegmentCompactor segmentCompactor = compactorMock(failed);
//
//    // Act
//    /// Activate compaction
//    segmentManager.write("key", "value");
//    /// Verify post update changes
//    segmentManager.write("key", "value");
//
//    // Assert
//    verify(newWritableSegment, times(1)).write("key", "value");
//    verify(segmentCompactor, times(2)).compactSegments();
//    verify(segmentDeleterFactory, times(0)).create(any());
//  }
//
//  @Test
//  void write_compaction_unexpectedFailure() throws Exception {
//    // Arrange
//    /// Activate compaction initiation
//    Segment writableSegment = mock(Segment.class);
//    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
//        mock(Segment.class));
//    Segment newWritableSegment = compactionInitiateMocks(writableSegment, frozenSegments);
//    /// Enable compaction mocking for unexpected failure
//    SegmentCompactor segmentCompactor = mock(SegmentCompactor.class);
//    doReturn(segmentCompactor).when(segmentCompactorFactory).create(any());
//    doReturn(immediateFailedFuture(new Exception("unexpected"))).when(segmentCompactor)
//        .compactSegments();
//
//    // Act
//    /// Activate compaction
//    segmentManager.write("key", "value");
//    /// Verify post update changes
//    segmentManager.write("key", "value");
//
//    // Assert
//    verify(segmentCompactor, times(1)).compactSegments();
//    verify(segmentDeleterFactory, times(0)).create(any());
//  }
//
//  @Test
//  void write_deletion_failedGeneral() throws Exception {
//    // Arrange
//    /// Activate compaction initiation
//    Segment writableSegment = mock(Segment.class);
//    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
//        mock(Segment.class));
//    compactionInitiateMocks(writableSegment, frozenSegments);
//    /// Enable compaction mocking
//    Segment compactedSegment = mock(Segment.class);
//    ImmutableList<Segment> providedSegments = ImmutableList.of(writableSegment,
//        frozenSegments.get(0), frozenSegments.get(1));
//    CompactionResults success = new CompactionResults.Success(
//        providedSegments, ImmutableList.of(compactedSegment));
//    compactorMock(success);
//    /// Mock Deleter for general failure
//    ImmutableList<Segment> segmentsProvidedForDeletion =
//        ImmutableList.of(mock(Segment.class), mock(Segment.class));
//    Throwable failureReason = mock(Throwable.class);
//    doReturn("generalFailure").when(failureReason).getMessage();
//    DeletionResults generalFailureResults = new DeletionResults.FailedGeneral(
//        segmentsProvidedForDeletion, failureReason);
//    SegmentDeleter segmentDeleter = deleterMock(generalFailureResults);
//
//    // Act
//    segmentManager.write("key", "value");
//
//    // Assert
//    verify(segmentDeleter, times(1)).deleteSegments();
//  }
//
//  void compactionMockForDeletion() throws Exception {
//    Segment writableSegment = mock(Segment.class);
//    ImmutableList<Segment> frozenSegments = ImmutableList.of(mock(Segment.class),
//        mock(Segment.class));
//    compactionInitiateMocks(writableSegment, frozenSegments);
//    Segment compactedSegment = mock(Segment.class);
//    ImmutableList<Segment> providedSegments = ImmutableList.of(writableSegment,
//        frozenSegments.get(0), frozenSegments.get(1));
//    CompactionResults success = new CompactionResults.Success(
//        providedSegments, ImmutableList.of(compactedSegment));
//    compactorMock(success);
//  }
//
//  @Test
//  void write_deletion_failedSegment() throws Exception {
//    // Arrange
//    /// Enable compaction mocking
//    compactionMockForDeletion();
//    /// Mock Deleter for segment failure
//    Segment firstSegment = mock(Segment.class);
//    Segment secondSegment = mock(Segment.class);
//    ImmutableList<Segment> segmentsProvidedForDeletion =
//        ImmutableList.of(firstSegment, secondSegment);
//    ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap =
//        ImmutableMap.of(
//            firstSegment, new IOException("deletion0 failed"),
//            secondSegment, new InterruptedException("deletion1 failed"));
//    DeletionResults segmentFailureResults =
//        new DeletionResults.FailedSegments(segmentsProvidedForDeletion, segmentsFailureReasonsMap);
//    SegmentDeleter segmentDeleter = deleterMock(segmentFailureResults);
//
//    // Act
//    segmentManager.write("key", "value");
//
//    // Assert
//    verify(segmentDeleter, times(1)).deleteSegments();
//  }
//
//  @Test
//  void write_deletion_unexpectedFailure() throws Exception {
//    // Arrange
//    /// Enable compaction mocking
//    compactionMockForDeletion();
//    /// Mock Deleter for unexpected failure
//    SegmentDeleter segmentDeleter = mock(SegmentDeleter.class);
//    doReturn(segmentDeleter).when(segmentDeleterFactory).create(any());
//    doReturn(immediateFailedFuture(new Exception("unexpected"))).when(segmentDeleter)
//        .deleteSegments();
//
//    // Act
//    segmentManager.write("key", "value");
//
//    // Assert
//    verify(segmentDeleter, times(1)).deleteSegments();
//  }
//
//  @Test
//  void close() {
//    Segment writableSegment = mock(Segment.class);
//    Segment frozenSegment = mock(Segment.class);
//    doReturn(writableSegment).when(managedSegments).getWritableSegment();
//    doReturn(ImmutableList.of(frozenSegment)).when(managedSegments).getFrozenSegments();
//    segmentManager.close();
//    verify(writableSegment, times(1)).close();
//    verify(frozenSegment, times(1)).close();
//  }
//}
