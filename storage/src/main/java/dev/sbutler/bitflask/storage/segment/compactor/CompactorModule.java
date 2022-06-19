package dev.sbutler.bitflask.storage.segment.compactor;

import com.google.inject.AbstractModule;

public class CompactorModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SegmentCompactor.class).toProvider(SegmentCompactorProvider.class);
  }
}
