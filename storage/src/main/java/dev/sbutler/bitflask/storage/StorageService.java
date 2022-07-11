package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.StorageResponse.Status;
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
public final class StorageService extends AbstractService implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentManager segmentManager;
  private final StorageCommandDispatcher commandDispatcher;
  private volatile boolean isRunning = true;

  @Inject
  public StorageService(@StorageExecutorService ListeningExecutorService executorService,
      SegmentManager segmentManager, StorageCommandDispatcher commandDispatcher) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
    this.commandDispatcher = commandDispatcher;
  }

  @Override
  public void run() {
    // TODO: bound executor task acceptance
    while (isRunning) {
      try {
        DispatcherSubmission<StorageCommand, StorageResponse> submission =
            commandDispatcher.poll(100, TimeUnit.MILLISECONDS);
        if (submission != null) {
          processSubmission(submission);
        }
      } catch (InterruptedException e) {
        logger.atWarning().withCause(e).log("Interrupted while polling dispatcher");
      }
    }
  }

  private void processSubmission(DispatcherSubmission<StorageCommand, StorageResponse> submission) {
    StorageCommand command = submission.command();
    SettableFuture<StorageResponse> response = submission.responseFuture();
    switch (command.type()) {
      case READ -> response.setFuture(read(command.arguments().get(0)));
      case WRITE ->
          response.setFuture(write(command.arguments().get(0), command.arguments().get(1)));
    }
  }

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  public ListenableFuture<StorageResponse> write(String key, String value) {
    Callable<StorageResponse> writeTask = () -> {
      try {
        segmentManager.write(key, value);
        logger.atInfo().log("Successful write of [%s]:[%s]", key, value);
        return new StorageResponse(Status.OK, Optional.of("Ok"), Optional.empty());
      } catch (Exception e) {
        logger.atWarning().withCause(e).log("Failed to write [%s]:[%s]", key, value);
        return new StorageResponse(Status.FAILED, Optional.empty(),
            Optional.of(String.format("Failure to write [%s]:[%s]", key, value)));
      }
    };

    logger.atInfo().log("Submitting write for [%s] : [%s]", key, value);
    return Futures.submit(writeTask, executorService);
  }

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  public ListenableFuture<StorageResponse> read(String key) {
    Callable<StorageResponse> readTask = () -> {
      try {
        Optional<String> value = segmentManager.read(key);
        logger.atInfo().log("Successful read of [%s]:[%s]", key, value);
        return new StorageResponse(Status.OK, value, Optional.empty());
      } catch (Exception e) {
        logger.atWarning().withCause(e).log("Failed to read [%s]", key);
        return new StorageResponse(Status.FAILED, Optional.empty(),
            Optional.of(String.format("Failure to read [%s]", key)));
      }
    };
    logger.atInfo().log("Submitting read for [%s]", key);
    return Futures.submit(readTask, executorService);
  }

  @Override
  protected void doStart() {
    ListenableFuture<Void> segmentManagerFuture =
        Futures.submit(() -> {
          segmentManager.initialize();
          return null;
        }, executorService);
    Runnable storageService = this;
    Futures.addCallback(segmentManagerFuture, new FutureCallback<>() {
      @Override
      public void onSuccess(Void result) {
        Futures.submit(storageService, executorService);
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
    System.out.println("StorageService shutdown triggered");
    isRunning = false;
    commandDispatcher.closeAndDrain();

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

    System.out.println("StorageService stopped");
    notifyStopped();
  }
}
