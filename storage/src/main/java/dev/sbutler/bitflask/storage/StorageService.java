package dev.sbutler.bitflask.storage;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.ReadCommand;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.commands.WriteCommand;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.time.Duration;
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
    StorageCommand command = dtoToCommandMapper(submission.commandDTO());
    SettableFuture<StorageResponse> response = submission.responseFuture();
    response.setFuture(command.execute());
  }

  private StorageCommand dtoToCommandMapper(StorageCommandDTO commandDTO) {
    return switch (commandDTO) {
      case ReadDTO readDTO -> new ReadCommand(executorService, segmentManager, readDTO);
      case WriteDTO writeDTO -> new WriteCommand(executorService, segmentManager, writeDTO);
    };
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
