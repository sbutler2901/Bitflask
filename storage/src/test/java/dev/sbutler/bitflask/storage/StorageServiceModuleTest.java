package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import org.junit.jupiter.api.Test;

public class StorageServiceModuleTest {

  private final StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();

  @Test
  void configure() {
    Injector injector = Guice.createInjector(StorageServiceModule.getInstance());
    try {
      injector.getBinding(
          Key.get(ListeningExecutorService.class).withAnnotation(StorageExecutorService.class));
      injector.getBinding(StorageService.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
