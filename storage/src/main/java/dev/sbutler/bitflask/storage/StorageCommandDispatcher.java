package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.storage.configuration.StorageDispatcherCapacity;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StorageCommandDispatcher {

  private final BlockingDeque<StorageSubmission> submissions;
  private volatile boolean isOpen = true;

  @Inject
  StorageCommandDispatcher(@StorageDispatcherCapacity int capacity) {
    submissions = new LinkedBlockingDeque<>(capacity);
  }

  /**
   * Submits the provide command to the dispatcher, waiting if necessary for space to be available
   *
   * <p>The future will fail with a {@link DispatcherClosedException} if the dispatcher is no
   * longer accepting new submissions.
   *
   * @return a future resolving with the StorageResponse, or an InterruptedException if interrupted
   * prior to submission
   */
  public ListenableFuture<StorageResponse> put(StorageCommand storageCommand) {
    SettableFuture<StorageResponse> response = SettableFuture.create();
    if (!isOpen) {
      response.setException(new DispatcherClosedException());
      return response;
    }

    try {
      submissions.putLast(new StorageSubmission(storageCommand, response));
    } catch (InterruptedException e) {
      response.setException(e);
    }
    return response;
  }

  /**
   * Submits the provided command to the dispatcher if space is available.
   *
   * <p>The future will fail with an {@link IllegalStateException} if the submission could not be
   * immediately submitted, or a {@link DispatcherClosedException} if the dispatcher is no longer
   * accepting new submissions.
   *
   * @return a future resolving with the StorageResponse, or an IllegalStateException if space was
   * not available
   */
  public ListenableFuture<StorageResponse> offer(StorageCommand storageCommand) {
    SettableFuture<StorageResponse> response = SettableFuture.create();
    if (!isOpen) {
      response.setException(new DispatcherClosedException());
      return response;
    }

    if (!submissions.offerLast(new StorageSubmission(storageCommand, response))) {
      response.setException(new IllegalStateException("Dispatcher queue is full"));
    }
    return response;
  }

  StorageSubmission poll(long timeout, TimeUnit unit) throws InterruptedException {
    return submissions.pollFirst(timeout, unit);
  }

  void closeAndDrain() {
    isOpen = false;
    while (!submissions.isEmpty()) {
      StorageSubmission submission = submissions.pollFirst();
      if (submission != null) {
        submission.response().setException(new DispatcherClosedException());
      }
    }
  }
}
