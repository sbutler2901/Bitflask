package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;

class SegmentCompactorProvider implements Provider<SegmentCompactor> {

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentCompactorProvider(@StorageExecutorService ExecutorService executorService,
      SegmentFactory segmentFactory) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public SegmentCompactor get() {
    return new SegmentCompactorImpl(executorService, segmentFactory);
  }

}
