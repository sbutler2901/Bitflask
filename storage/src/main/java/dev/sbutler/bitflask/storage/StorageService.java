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
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
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
  protected void run() throws Exception {
    while (isRunning) {
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
          commandDispatcher.poll(1, TimeUnit.SECONDS);
      if (submission != null) {
        processSubmission(submission);
      }
    }
  }

  private void processSubmission(
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission) {
    // TODO: bound executor task acceptance
    StorageCommandDTO commandDTO = submission.commandDTO();
    SettableFuture<StorageResponse> response = submission.responseFuture();
    // TODO: map to command with execute
    switch (commandDTO) {
      case ReadDTO readDTO -> response.setFuture(read(readDTO.key()));
      case WriteDTO writeDTO -> response.setFuture(write(writeDTO.key(), writeDTO.value()));
    }
  }

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  private ListenableFuture<StorageResponse> read(String key) {
    Callable<StorageResponse> readTask = () -> {
      try {
        Optional<String> value = segmentManager.read(key);
        logger.atInfo().log("Successful read of [%s]:[%s]", key, value);
        if (value.isEmpty()) {
          value = Optional.of(String.format("[%s] not found", key));
        }
        return new Success(value.get());
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("Failed to read [%s]", key);
        return new Failed(String.format("Failure to read [%s]", key));
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
  private ListenableFuture<StorageResponse> write(String key, String value) {
    Callable<StorageResponse> writeTask = () -> {
      try {
        segmentManager.write(key, value);
        logger.atInfo().log("Successful write of [%s]:[%s]", key, value);
        return new Success("OK");
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("Failed to write [%s]:[%s]", key, value);
        return new Failed(String.format("Failure to write [%s]:[%s]", key, value));
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
