package dev.sbutler.bitflask.storage;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ServiceManager;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages persisting and retrieving data.
 */
@Singleton
public final class StorageService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ServiceManager serviceManager;
  private final ListeningExecutorService listeningExecutorService;
  private final StorageCommandDispatcher commandDispatcher;
  private final CommandMapper commandMapper;
  private volatile boolean shouldContinueRunning = true;

  @Inject
  public StorageService(ListeningExecutorService listeningExecutorService,
      SegmentManagerService segmentManagerService, StorageCommandDispatcher commandDispatcher,
      CommandMapper commandMapper) {
    this.listeningExecutorService = listeningExecutorService;
    this.commandDispatcher = commandDispatcher;
    this.commandMapper = commandMapper;
    this.serviceManager = new ServiceManager(ImmutableSet.of(segmentManagerService));
  }

  @Override
  protected void startUp() {
    logger.atFine().log("StorageService: startUp start");
    serviceManager.startAsync();
    registerShutdownListener();
    serviceManager.awaitHealthy();
    logger.atFine().log("StorageService: startUp end");
  }

  @Override
  public void run() throws Exception {
    logger.atFine().log("StorageService: starting run");
    while (shouldContinueRunning && !Thread.currentThread().isInterrupted()) {
      Optional<DispatcherSubmission<StorageCommandDTO, StorageResponse>> submission =
          commandDispatcher.poll(1, TimeUnit.SECONDS);
      submission.ifPresent(this::processSubmission);
    }
    logger.atFine().log("StorageService: stopping run");
    System.out.println("StorageService: stopping run");
  }

  private void processSubmission(
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission) {
    StorageCommand command = commandMapper.mapToCommand(submission.commandDTO());
    submission.responseFuture().setFuture(command.execute());
  }

  private void registerShutdownListener() {
    this.addListener(new Listener() {
      @Override
      public void running() {
        logger.atFine().log("StorageService.Listener: running");
      }

      @SuppressWarnings("UnstableApiUsage")
      @Override
      public void stopping(State from) {
        super.stopping(from);
        logger.atFine().log("StorageService.Listener: stopping started, from state: %s", from);

        shouldContinueRunning = false;
        commandDispatcher.closeAndDrain();
        stopServices();
        shutdownAndAwaitTermination(listeningExecutorService, Duration.ofSeconds(5));
        System.out.println("StorageService.Listener: stopping finished");
      }

      @Override
      public void failed(State from, Throwable failure) {
        super.failed(from, failure);
        logger.atFine().withCause(failure)
            .log("StorageService.Listener: failed, from state: %s", from);
      }
    }, listeningExecutorService);
  }

  private void stopServices() {
    // Give the services 5 seconds to stop to ensure that we are responsive to shut down
    // requests.
    try {
      serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
    } catch (TimeoutException timeout) {
      // stopping timed out
      logger.atWarning().log("StorageService: ServiceManager timed out while stopping");
    }
  }
}
