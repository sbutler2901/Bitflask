package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
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
    SegmentManager segmentManagerImpl = mock(SegmentManager.class);
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
  void provideManagedSegments() throws Exception {
    // Arrange
    SegmentModule segmentModule = new SegmentModule();
    SegmentLoader segmentLoader = mock(SegmentLoader.class);
    ManagedSegments managedSegmentsMock = mock(ManagedSegments.class);
    doReturn(managedSegmentsMock).when(segmentLoader).loadExistingSegments();
    // Act
    ManagedSegments managedSegments = segmentModule.provideManagedSegments(segmentLoader);
    // Assert
    assertEquals(managedSegmentsMock, managedSegments);
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      bind(ListeningExecutorService.class).annotatedWith(StorageExecutorService.class)
          .toProvider(mock(Provider.class));
    }

  }
}
