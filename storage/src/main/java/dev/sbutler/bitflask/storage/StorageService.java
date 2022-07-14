package dev.sbutler.bitflask.storage;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Status;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages persisting and retrieving data.
 */
@Singleton
public final class StorageService extends AbstractExecutionThreadService {

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
  protected void startUp() throws Exception {
    segmentManager.initialize();
    logger.atInfo().log("StorageService started");
  }

  @Override
  public void run() throws Exception {
    while (isRunning) {
      DispatcherSubmission<StorageCommand, StorageResponse> submission =
          commandDispatcher.poll(1, TimeUnit.SECONDS);
      if (submission != null) {
        processSubmission(submission);
      }
    }
  }

  private void processSubmission(DispatcherSubmission<StorageCommand, StorageResponse> submission) {
    // TODO: bound executor task acceptance
    StorageCommand command = submission.command();
    SettableFuture<StorageResponse> response = submission.responseFuture();
    switch (command.type()) {
      case READ -> response.setFuture(read(command.arguments().get(0)));
      case WRITE ->
          response.setFuture(write(command.arguments().get(0), command.arguments().get(1)));
    }
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
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("Failed to read [%s]", key);
        return new StorageResponse(Status.FAILED, Optional.empty(),
            Optional.of(String.format("Failure to read [%s]", key)));
      }
    };
    logger.atInfo().log("Submitting read for [%s]", key);
    return Futures.submit(readTask, executorService);
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
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("Failed to write [%s]:[%s]", key, value);
        return new StorageResponse(Status.FAILED, Optional.empty(),
            Optional.of(String.format("Failure to write [%s]:[%s]", key, value)));
      }
    };

    logger.atInfo().log("Submitting write for [%s] : [%s]", key, value);
    return Futures.submit(writeTask, executorService);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    System.out.println("StorageService shutdown triggered");
    isRunning = false;
    commandDispatcher.closeAndDrain();
    segmentManager.close();
    shutdownAndAwaitTermination(executorService, Duration.ofSeconds(5));
    System.out.println("StorageService completed shutdown");
  }
}
