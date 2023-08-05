package dev.sbutler.bitflask.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.guice.RootModule;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.lsm.LSMTreeModule;
import java.util.concurrent.ThreadFactory;

/** The root Guice module for executing the StorageService */
public class StorageServiceModule extends RootModule {

  public StorageServiceModule() {}

  @Override
  protected void configure() {
    install(new LSMTreeModule());
  }

  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of(injector.getInstance(StorageService.class));
  }

  @Provides
  FilesHelper provideFilesHelper(ThreadFactory threadFactory) {
    return new FilesHelper(threadFactory);
  }
}
