package dev.sbutler.bitflask.storage.segment;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import java.io.IOException;
import javax.inject.Singleton;

public class SegmentModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
        .implement(SegmentCompactor.class, SegmentCompactorImpl.class)
        .build(SegmentCompactorFactory.class));
    install(new FactoryModuleBuilder()
        .implement(SegmentDeleter.class, SegmentDeleterImpl.class)
        .build(SegmentDeleterFactory.class));
    install(new FactoryModuleBuilder()
        .implement(SegmentFile.class, SegmentFileImpl.class)
        .build(SegmentFileFactory.class));
  }

  @Provides
  @Singleton
  SegmentManager provideSegmentManager(SegmentManager segmentManager) {
    return segmentManager;
  }

  @Provides
  SegmentLoader provideSegmentLoader(SegmentLoaderImpl segmentLoader) {
    return segmentLoader;
  }

  @Provides
  @Singleton
  ManagedSegments provideManagedSegments(SegmentLoader segmentLoader) throws IOException {
    return segmentLoader.loadExistingSegments();
  }
}
