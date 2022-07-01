package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import dev.sbutler.bitflask.storage.segment.SegmentManagerImpl.ManagedSegmentsImpl;
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

  @Provides
  @Singleton
  ManagedSegments provideManagedSegments(SegmentFactory segmentFactory,
      SegmentLoader segmentLoader) throws IOException {
    boolean segmentStoreDirCreated = segmentFactory.createSegmentStoreDir();
    ImmutableList<Segment> loadedSegments = segmentStoreDirCreated ? ImmutableList.of()
        : segmentLoader.loadExistingSegments();

    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      writableSegment = segmentFactory.createSegment();
    } else if (loadedSegments.get(0).exceedsStorageThreshold()) {
      writableSegment = segmentFactory.createSegment();
    } else {
      writableSegment = loadedSegments.get(0);
      loadedSegments = loadedSegments.subList(1, loadedSegments.size());
    }

    return new ManagedSegmentsImpl(writableSegment, loadedSegments);
  }
}
