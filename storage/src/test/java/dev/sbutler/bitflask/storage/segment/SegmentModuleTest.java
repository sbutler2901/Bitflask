package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

public class SegmentModuleTest {

  @Test
  void configure() {
    // Arrange
    Injector injector = Guice.createInjector(new MockModule(), new SegmentModule());
    try {
      // act
      injector.getBinding(SegmentManager.class);
      // assert
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideSegmentManager() {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentManagerImpl segmentManagerImpl = mock(SegmentManagerImpl.class);
    // Act
    SegmentManager segmentManager = segmentModule.provideSegmentManager(segmentManagerImpl);
    // Assert
    assertEquals(segmentManagerImpl, segmentManager);
  }

  @Test
  void provideSegmentFactory() {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentFactoryImpl segmentFactoryImpl = mock(SegmentFactoryImpl.class);
    // Act
    SegmentFactory segmentFactory = segmentModule.provideSegmentFactory(segmentFactoryImpl);
    // Assert
    assertEquals(segmentFactoryImpl, segmentFactory);
  }

  @Test
  void provideSegmentLoader() {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentLoaderImpl segmentLoaderImpl = mock(SegmentLoaderImpl.class);
    // Act
    SegmentLoader segmentLoader = segmentModule.provideSegmentLoader(segmentLoaderImpl);
    // Assert
    assertEquals(segmentLoaderImpl, segmentLoader);
  }

  @Test
  void provideManagedSegments_emptyLoad() throws Exception {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentFactory segmentFactory = mock(SegmentFactory.class);
    SegmentLoader segmentLoader = mock(SegmentLoader.class);
    doReturn(true).when(segmentFactory).createSegmentStoreDir();
    Segment segment = mock(Segment.class);
    doReturn(segment).when(segmentFactory).createSegment();
    // Act
    ManagedSegments managedSegments = segmentModule.provideManagedSegments(segmentFactory,
        segmentLoader);
    // Assert
    assertEquals(segment, managedSegments.getWritableSegment());
    assertEquals(0, managedSegments.getFrozenSegments().size());
  }

  @Test
  void provideManagedSegments_firstLoadedExceedsStorageThreshold() throws Exception {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentFactory segmentFactory = mock(SegmentFactory.class);
    SegmentLoader segmentLoader = mock(SegmentLoader.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    Segment loadedSegment = mock(Segment.class);
    doReturn(true).when(loadedSegment).exceedsStorageThreshold();
    doReturn(ImmutableList.of(loadedSegment)).when(segmentLoader).loadExistingSegments();
    Segment createdSegment = mock(Segment.class);
    doReturn(createdSegment).when(segmentFactory).createSegment();
    // Act
    ManagedSegments managedSegments = segmentModule.provideManagedSegments(segmentFactory,
        segmentLoader);
    // Assert
    assertEquals(createdSegment, managedSegments.getWritableSegment());
    assertEquals(1, managedSegments.getFrozenSegments().size());
    assertEquals(loadedSegment, managedSegments.getFrozenSegments().get(0));
  }

  @Test
  void provideManagedSegments_firstLoadedAsWritable() throws Exception {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentFactory segmentFactory = mock(SegmentFactory.class);
    SegmentLoader segmentLoader = mock(SegmentLoader.class);
    doReturn(false).when(segmentFactory).createSegmentStoreDir();
    Segment loadedSegment0 = mock(Segment.class);
    Segment loadedSegment1 = mock(Segment.class);
    doReturn(false).when(loadedSegment0).exceedsStorageThreshold();
    doReturn(ImmutableList.of(loadedSegment0, loadedSegment1)).when(segmentLoader)
        .loadExistingSegments();
    // Act
    ManagedSegments managedSegments = segmentModule.provideManagedSegments(segmentFactory,
        segmentLoader);
    // Arrange
    assertEquals(loadedSegment0, managedSegments.getWritableSegment());
    assertEquals(1, managedSegments.getFrozenSegments().size());
    assertEquals(loadedSegment1, managedSegments.getFrozenSegments().get(0));
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      bind(ExecutorService.class).annotatedWith(StorageExecutorService.class)
          .toProvider(mock(Provider.class));
    }

  }
}
