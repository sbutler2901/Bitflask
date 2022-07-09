package dev.sbutler.bitflask.storage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages persisting and retrieving data.
 */
@Singleton
public final class StorageService extends AbstractService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentManager segmentManager;

  @Inject
  public StorageService(@StorageExecutorService ListeningExecutorService executorService,
      SegmentManager segmentManager) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
  }

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  public ListenableFuture<Void> write(String key, String value) {
    validateWriteArgs(key, value);
    Callable<Void> writeTask = () -> {
      segmentManager.write(key, value);
      return null;
    };
    logger.atInfo().log("Submitting write for [%s] : [%s]", key, value);
    return executorService.submit(writeTask);
  }

  private void validateWriteArgs(String key, String value) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
    checkNotNull(value);
    checkArgument(!value.isBlank(), "Expected non-blank key, but was [%s]", value);
    checkArgument(value.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        value.length());
  }

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  public ListenableFuture<Optional<String>> read(String key) {
    validateReadArgs(key);
    Callable<Optional<String>> readTask = () -> segmentManager.read(key);
    logger.atInfo().log("Submitting read for [%s]", key);
    return executorService.submit(readTask);
  }

  private void validateReadArgs(String key) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
  }

  @Override
  protected void doStart() {
    ListenableFuture<Void> segmentManagerFuture =
        Futures.submit(() -> {
          segmentManager.initialize();
          return null;
        }, executorService);
    Futures.addCallback(segmentManagerFuture, new FutureCallback<>() {
      @Override
      public void onSuccess(Void result) {
        notifyStarted();
      }

      @Override
      public void onFailure(@Nonnull Throwable t) {
        notifyFailed(t);
      }
    }, executorService);
  }

  @Override
  protected void doStop() {
    InterruptedException interruptedException = null;
    boolean shutdownBeforeTimeoutOrInterruption = false;

    // Disable new submissions to service
    executorService.shutdown();
    try {
      // Wait for existing tasks to complete
      shutdownBeforeTimeoutOrInterruption =
          executorService.awaitTermination(500L, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      interruptedException = e;
    } finally {
      // Close all Segments
      segmentManager.close();
    }

    if (interruptedException != null) {
      // Preserve interrupt status
      Thread.currentThread().interrupt();
      notifyFailed(interruptedException);
      return;
    }

    if (!shutdownBeforeTimeoutOrInterruption) {
      // Cancel currently executing tasks
      executorService.shutdownNow();
      try {
        // Wait for tasks to respond to being canceled
        if (!executorService.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
          notifyFailed(new TimeoutException("StorageService's ExecutorService did not shutdown"));
        }
      } catch (InterruptedException e) {
        // Preserve interrupt status
        Thread.currentThread().interrupt();
        notifyFailed(e);
        return;
      }
    }

    notifyStopped();
  }
}
