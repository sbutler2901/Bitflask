package dev.sbutler.bitflask.storage.segment;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class SegmentModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
        .implement(SegmentDeleter.class, SegmentDeleterImpl.class)
        .build(SegmentDeleterFactory.class));
    install(new FactoryModuleBuilder()
        .implement(SegmentFile.class, SegmentFileImpl.class)
        .build(SegmentFileFactory.class));
  }
}
