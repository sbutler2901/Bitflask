package dev.sbutler.bitflask.storage.segment;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import javax.inject.Singleton;

public class SegmentModule extends AbstractModule {

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

}
