package dev.sbutler.bitflask.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.guice.RootModule;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.lsm.LSMTreeModule;
import dev.sbutler.bitflask.storage.raft.RaftModule;
import java.util.concurrent.ThreadFactory;

/** The root Guice module for executing the StorageService */
public class StorageServiceModule extends RootModule {

  private final RaftModule raftModule = new RaftModule();

  public StorageServiceModule() {}

  @Override
  protected void configure() {
    install(new LSMTreeModule());
    install(raftModule);
  }

  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.<Service>builder()
        .add(injector.getInstance(StorageService.class))
        .addAll(raftModule.getServices(injector))
        .build();
  }

  @Provides
  FilesHelper provideFilesHelper(ThreadFactory threadFactory) {
    return new FilesHelper(threadFactory);
  }
}
