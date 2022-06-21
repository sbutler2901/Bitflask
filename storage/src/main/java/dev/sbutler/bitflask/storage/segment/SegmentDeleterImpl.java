package dev.sbutler.bitflask.storage.segment;

import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;

class SegmentDeleterImpl implements SegmentDeleter {

  private final ExecutorService executorService;
  private final List<Segment> segmentsToBeDeleted;
  private final List<Consumer<DeletionResults>> deletionResultsConsumers = new ArrayList<>();
  private volatile boolean deletionStarted = false;

  @Inject
  SegmentDeleterImpl(@StorageExecutorService ExecutorService executorService,
      @Assisted List<Segment> segmentsToBeDeleted) {
    this.executorService = executorService;
    this.segmentsToBeDeleted = segmentsToBeDeleted;
  }

  @Override
  public void deleteSegments() {
    if (deletionStarted) {
      return;
    }

    deletionStarted = true;
    // todo: implement deletion logic
  }

  @Override
  public void registerDeletionResultsConsumer(Consumer<DeletionResults> deletionResultsConsumer) {
    deletionResultsConsumers.add(deletionResultsConsumer);
  }

  private void runRegisteredDeletionResultsConsumers(DeletionResults deletionResults) {
    deletionResultsConsumers.forEach(consumer -> consumer.accept(deletionResults));
  }
}
