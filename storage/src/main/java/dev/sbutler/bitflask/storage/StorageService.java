package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages persisting and retrieving data.
 */
@Singleton
public final class StorageService extends AbstractExecutionThreadService {

  private final ListeningExecutorService executorService;
  private final StorageCommandDispatcher commandDispatcher;
  private final CommandMapper commandMapper;

  private volatile boolean isRunning = true;

  @Inject
  public StorageService(ListeningExecutorService executorService,
      StorageCommandDispatcher commandDispatcher,
      CommandMapper commandMapper) {
    this.executorService = executorService;
    this.commandDispatcher = commandDispatcher;
    this.commandMapper = commandMapper;
  }

  @Override
  protected void startUp() {
    // TODO: load LSMTree
  }

  @Override
  public void run() {
    try {
      while (isRunning && !Thread.currentThread().isInterrupted()) {
        Optional<DispatcherSubmission<StorageCommandDTO, StorageResponse>> submission =
            commandDispatcher.poll(1, TimeUnit.SECONDS);
        submission.ifPresent(this::processSubmission);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StorageException(e);
    } finally {
      triggerShutdown();
    }
  }

  private void processSubmission(
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission) {
    StorageCommand command = commandMapper.mapToCommand(submission.commandDTO());
    submission.responseFuture().setFuture(Futures.submit(command::execute, executorService));
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    isRunning = false;
    commandDispatcher.closeAndDrain();
  }
}
