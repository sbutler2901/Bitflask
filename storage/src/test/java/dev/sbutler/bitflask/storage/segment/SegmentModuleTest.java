package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

public class SegmentModuleTest {

  @Test
  void configure() {
    Injector injector = Guice.createInjector(new MockModule(), new SegmentModule());
    try {
      injector.getBinding(SegmentManager.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideSegmentManager() {
    SegmentModule segmentModule = new SegmentModule();
    SegmentManagerImpl segmentManagerImpl = mock(SegmentManagerImpl.class);
    SegmentManager segmentManager = segmentModule.provideSegmentManager(segmentManagerImpl);
    assertEquals(segmentManagerImpl, segmentManager);
    assertInstanceOf(SegmentManager.class, segmentManager);
  }

  @Test
  void provideSegmentFactory() {
    SegmentModule segmentModule = new SegmentModule();
    SegmentFactoryImpl segmentFactoryImpl = mock(SegmentFactoryImpl.class);
    SegmentFactory segmentFactory = segmentModule.provideSegmentFactory(segmentFactoryImpl);
    assertEquals(segmentFactoryImpl, segmentFactory);
    assertInstanceOf(SegmentFactory.class, segmentFactory);
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      bind(ExecutorService.class)
          .annotatedWith(StorageExecutorService.class)
          .toProvider(mock(Provider.class));
    }

  }
}
