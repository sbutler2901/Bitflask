package dev.sbutler.bitflask.storage.segment;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import javax.inject.Singleton;

public class SegmentModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
        .implement(SegmentCompactor.class, SegmentCompactorImpl.class)
        .build(SegmentCompactorFactory.class));
  }

  @Provides
  @Singleton
  SegmentManager provideSegmentManager(SegmentManagerImpl segmentManager) {
    return segmentManager;
  }

  @Provides
  @Singleton
  SegmentFactory provideSegmentFactory(SegmentFactoryImpl segmentFactory) {
    return segmentFactory;
  }

  @Provides
  SegmentLoader provideSegmentLoader(SegmentLoaderImpl segmentLoader) {
    return segmentLoader;
  }

}
