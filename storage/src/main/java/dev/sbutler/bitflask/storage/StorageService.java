package dev.sbutler.bitflask.storage;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages persisting and retrieving data.
 */
@Singleton
public final class StorageService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ServiceManager serviceManager;
  private final ListeningExecutorService executorService;
  private final StorageCommandDispatcher commandDispatcher;
  private final CommandMapper commandMapper;
  private volatile boolean isRunning = true;

  @Inject
  public StorageService(@StorageExecutorService ListeningExecutorService executorService,
      SegmentManagerService segmentManagerService, StorageCommandDispatcher commandDispatcher,
      CommandMapper commandMapper) {
    this.executorService = executorService;
    this.commandDispatcher = commandDispatcher;
    this.commandMapper = commandMapper;
    this.serviceManager = new ServiceManager(ImmutableSet.of(segmentManagerService));
  }

  @Override
  protected void startUp() {
    serviceManager.awaitHealthy();
    logger.atInfo().log("StorageService started");
  }

  @Override
  protected void run() throws Exception {
    while (isRunning) {
      // TODO: bound executor task acceptance
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
          commandDispatcher.poll(1, TimeUnit.SECONDS);
      if (submission != null) {
        processSubmission(submission);
      }
    }
  }

  private void processSubmission(DispatcherSubmission<StorageCommandDTO,
      StorageResponse> submission) {
    StorageCommand command = commandMapper.mapToCommand(submission.commandDTO());
    submission.responseFuture().setFuture(command.execute());
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    System.out.println("StorageService shutdown triggered");
    isRunning = false;
    commandDispatcher.closeAndDrain();
    stopServices();
    shutdownAndAwaitTermination(executorService, Duration.ofSeconds(5));
    System.out.println("StorageService completed shutdown");
  }

  private void stopServices() {
    // Give the services 5 seconds to stop to ensure that we are responsive to shut down
    // requests.
    try {
      serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
    } catch (TimeoutException timeout) {
      // stopping timed out
      System.err.println("StorageService's ServiceManager timed out while stopping" + timeout);
    }
  }

  private void addServiceManagerListener() {
    serviceManager.addListener(
        new ServiceManager.Listener() {
          public void stopped() {
            System.out.println("StorageService: All services have stopped.");
          }

          public void healthy() {
            logger.atInfo().log("All services have been initialized and are healthy");
          }

          public void failure(@Nonnull Service service) {
            System.err.printf("StorageService: [%s] failed to start.", service.getClass());
          }
        }, executorService);
  }
}
