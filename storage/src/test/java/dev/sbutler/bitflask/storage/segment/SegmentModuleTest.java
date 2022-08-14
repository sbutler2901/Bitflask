package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import org.junit.jupiter.api.Test;

public class SegmentModuleTest {

  @Test
  void configure() {
    // Arrange
    Injector injector = Guice.createInjector(new MockModule(), new SegmentModule());
    try {
      // act
      injector.getBinding(SegmentCompactorFactory.class);
      injector.getBinding(SegmentDeleterFactory.class);
      injector.getBinding(SegmentFileFactory.class);
      // assert
    } catch (Exception e) {
      fail(e.getMessage());
    }
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
