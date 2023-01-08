package dev.sbutler.bitflask.storage;

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
    serviceManager.startAsync();
    registerShutdownListener();
    serviceManager.awaitHealthy();
  }

  @Override
  public void run() throws Exception {
    while (isRunning() && !Thread.currentThread().isInterrupted()) {
      Optional<DispatcherSubmission<StorageCommandDTO, StorageResponse>> submission =
          commandDispatcher.poll(1, TimeUnit.SECONDS);
      submission.ifPresent(this::processSubmission);
    }
  }

  private void processSubmission(
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission) {
    StorageCommand command = commandMapper.mapToCommand(submission.commandDTO());
    submission.responseFuture().setFuture(command.execute());
  }

  private void registerShutdownListener() {
    this.addListener(new Listener() {
      @SuppressWarnings("NullableProblems")
      @Override
      public void stopping(State from) {
        super.stopping(from);
        commandDispatcher.closeAndDrain();
        stopServices();
      }
    }, listeningExecutorService);
  }

  private void stopServices() {
    // Give the services 5 seconds to stop to ensure that we are responsive to shut down
    // requests.
    try {
      serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      logger.atWarning().withCause(e)
          .log("StorageService: ServiceManager timed out while stopping");
    }
  }
}
