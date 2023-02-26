package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages persisting and retrieving data.
 */
@Singleton
public final class StorageService extends AbstractExecutionThreadService {

  private final SegmentManagerService segmentManagerService;
  private final StorageCommandDispatcher commandDispatcher;
  private final CommandMapper commandMapper;

  private volatile boolean isRunning = true;

  @Inject
  public StorageService(SegmentManagerService segmentManagerService,
      StorageCommandDispatcher commandDispatcher, CommandMapper commandMapper) {
    this.segmentManagerService = segmentManagerService;
    this.commandDispatcher = commandDispatcher;
    this.commandMapper = commandMapper;
  }

  @Override
  protected void startUp() {
    segmentManagerService.startAsync();
    segmentManagerService.awaitRunning();
  }

  @Override
  public void run() throws Exception {
    try {
      while (isRunning && segmentManagerService.isRunning()
          && !Thread.currentThread().isInterrupted()) {
        Optional<DispatcherSubmission<StorageCommandDTO, StorageResponse>> submission =
            commandDispatcher.poll(1, TimeUnit.SECONDS);
        submission.ifPresent(this::processSubmission);
      }
    } finally {
      triggerShutdown();
    }
  }

  private void processSubmission(
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission) {
    StorageCommand command = commandMapper.mapToCommand(submission.commandDTO());
    submission.responseFuture().setFuture(command.execute());
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    isRunning = false;
    commandDispatcher.closeAndDrain();
    segmentManagerService.stopAsync();
    segmentManagerService.awaitTerminated();
  }
}
