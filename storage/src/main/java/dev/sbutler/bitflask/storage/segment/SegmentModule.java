package dev.sbutler.bitflask.storage.segment;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.storage.segment.compactor.CompactorModule;
import javax.inject.Singleton;

public class SegmentModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new CompactorModule());
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
